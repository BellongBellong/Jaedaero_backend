package com.jaedaero.domain.strategyapplication.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResult;
import com.jaedaero.domain.aianalysis.dto.AiRecommendedScenarioResponse;
import com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper;
import com.jaedaero.domain.aianalysis.service.AiAnalysisInputProvider;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisVo;
import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.domain.strategyapplication.exception.StrategyApplicationErrorCode;
import com.jaedaero.domain.strategyapplication.exception.StrategyApplicationException;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.strategyapplication.service.StrategyApplicationService;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationSourceType;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StrategyApplicationServiceImpl implements StrategyApplicationService {

  private final StrategyApplicationMapper strategyApplicationMapper;
  private final AiAnalysisMapper aiAnalysisMapper;
  private final AiAnalysisInputProvider aiAnalysisInputProvider;
  private final CashflowService cashflowService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public StrategyApplicationResponse applyAiRecommendation(long userId, long analysisId) {
    AiAnalysisVo analysis = aiAnalysisMapper.findAnalysisByIdAndUserId(analysisId, userId);
    if (analysis == null) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.NOT_FOUND, "적용할 AI 분석 결과를 찾을 수 없습니다.");
    }

    AiRecommendedScenarioVo recommendation = recommendation(analysis, userId);
    long beforeExpectedAsset = aiAnalysisInputProvider.load(userId).expectedAsset();

    StrategyApplicationVo application =
        StrategyApplicationVo.builder()
            .userId(userId)
            .sourceType(StrategyApplicationSourceType.AI_RECOMMENDATION)
            .aiScenarioId(recommendation.getScenarioId())
            .appliedMonthlySavingAmount(recommendation.getMonthlySavingAmount())
            .appliedMonthlyInvestmentAmount(recommendation.getMonthlyInvestmentAmount())
            .appliedExpectedReturnRate(recommendation.getExpectedReturnRate())
            .appliedMonthlySpendingAmount(recommendation.getMonthlySpendingAmount())
            .beforeExpectedAsset(beforeExpectedAsset)
            .build();
    strategyApplicationMapper.insert(application);

    // 최신 strategy_application이 현재 적용 상태다. 새 상태로 캐시플로우를 즉시 다시 계산한다.
    CashflowForecastResponse recalculated = cashflowService.generate(userId);
    application.setAfterExpectedAsset(recalculated.getExpectedAsset());
    strategyApplicationMapper.updateAfterExpectedAsset(
        application.getApplicationId(), userId, recalculated.getExpectedAsset());

    StrategyApplicationVo saved =
        strategyApplicationMapper.findByIdAndUserId(application.getApplicationId(), userId);
    if (saved == null) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.NOT_FOUND, "저장된 전략 적용 이력을 찾을 수 없습니다.");
    }
    return StrategyApplicationResponse.from(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<StrategyApplicationResponse> getHistory(long userId, int page, int size) {
    return strategyApplicationMapper.findByUserId(userId, (long) page * size, size).stream()
        .map(StrategyApplicationResponse::from)
        .toList();
  }

  private AiRecommendedScenarioVo recommendation(AiAnalysisVo analysis, long userId) {
    AiRecommendedScenarioResponse embedded = read(analysis.getResultJson()).getRecommendedScenario();
    if (embedded == null || embedded.getScenarioId() == null) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.INVALID_SOURCE, "AI 분석 결과에 적용 가능한 추천 시나리오가 없습니다.");
    }
    AiRecommendedScenarioVo recommendation =
        aiAnalysisMapper.findRecommendedScenarioByIdAndUserId(embedded.getScenarioId(), userId);
    if (recommendation == null) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.INVALID_SOURCE, "AI 추천 시나리오가 존재하지 않거나 사용자와 일치하지 않습니다.");
    }
    return recommendation;
  }

  private AiAnalysisResult read(String resultJson) {
    try {
      return objectMapper.readValue(resultJson, AiAnalysisResult.class);
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.INVALID_SOURCE, "AI 분석 결과에서 추천 시나리오를 읽을 수 없습니다.", exception);
    }
  }
}
