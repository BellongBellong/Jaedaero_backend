package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;
import com.jaedaero.domain.aianalysis.dto.AiRecommendedScenarioResponse;
import com.jaedaero.domain.aianalysis.service.AiAnalysisService;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.service.SimulationService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * 추천 화면이 전달한 분석 ID를 사용자 소유권이 검증된 추천 조건으로 변환합니다.
 *
 * <p>하위 서비스의 상세 조회는 모두 userId를 함께 받아 다른 사용자의 분석/시뮬레이션을 조회할 수 없습니다.</p>
 */
@Component
public class ProductRecommendationContextResolver {

  private final AiAnalysisService aiAnalysisService;
  private final SimulationService simulationService;

  public ProductRecommendationContextResolver(
      AiAnalysisService aiAnalysisService, SimulationService simulationService) {
    this.aiAnalysisService = aiAnalysisService;
    this.simulationService = simulationService;
  }

  public ProductRecommendationContext resolve(long userId, Long analysisId, Long simulationId) {
    validate(analysisId, simulationId);
    if (analysisId == null && simulationId == null) {
      return null;
    }
    return analysisId != null ? fromAnalysis(userId, analysisId) : fromSimulation(userId, simulationId);
  }

  private ProductRecommendationContext fromAnalysis(long userId, long analysisId) {
    AiAnalysisResponse analysis = aiAnalysisService.getDetail(userId, analysisId);
    AiRecommendedScenarioResponse scenario = analysis.getRecommendedScenario();
    if (scenario == null || scenario.getMonthlyInvestmentAmount() == null) {
      throw new ProductRecommendationException(
          ProductRecommendationErrorCode.CONTEXT_NOT_READY,
          "AI 분석에 상품 추천에 사용할 월 투자금이 없습니다.");
    }
    LocalDate financialDischargeDate =
        scenario.getFinancialDischargeDate() == null
            ? analysis.getFinancialDischargeDate()
            : scenario.getFinancialDischargeDate();
    return new ProductRecommendationContext(
        analysis.getAnalysisId(),
        analysis.getSimulationId(),
        scenario.getMonthlyInvestmentAmount(),
        scenario.getExpectedReturnRate(),
        financialDischargeDate);
  }

  private ProductRecommendationContext fromSimulation(long userId, long simulationId) {
    SimulationResponse simulation = simulationService.getDetail(userId, simulationId);
    if (simulation.getMonthlyInvestmentAmount() == null) {
      throw new ProductRecommendationException(
          ProductRecommendationErrorCode.CONTEXT_NOT_READY,
          "What-if 시뮬레이션에 상품 추천에 사용할 월 투자금이 없습니다.");
    }
    return new ProductRecommendationContext(
        null,
        simulation.getSimulationId(),
        simulation.getMonthlyInvestmentAmount(),
        simulation.getExpectedReturnRate(),
        simulation.getFinancialDischargeDate());
  }

  private void validate(Long analysisId, Long simulationId) {
    if (analysisId != null && simulationId != null) {
      throw new ProductRecommendationException(
          ProductRecommendationErrorCode.INVALID_CONTEXT,
          "analysisId와 simulationId는 동시에 지정할 수 없습니다.");
    }
    if ((analysisId != null && analysisId <= 0) || (simulationId != null && simulationId <= 0)) {
      throw new ProductRecommendationException(
          ProductRecommendationErrorCode.INVALID_CONTEXT,
          "analysisId와 simulationId는 양수여야 합니다.");
    }
  }
}
