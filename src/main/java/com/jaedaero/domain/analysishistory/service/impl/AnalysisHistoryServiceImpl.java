package com.jaedaero.domain.analysishistory.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryFilter;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryItemResponse;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryPageResponse;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryType;
import com.jaedaero.domain.analysishistory.mapper.AnalysisHistoryMapper;
import com.jaedaero.domain.analysishistory.service.AnalysisHistoryService;
import com.jaedaero.domain.analysishistory.vo.AnalysisHistoryRow;
import com.jaedaero.domain.simulation.service.SimulationAllocationMetrics;
import com.jaedaero.domain.simulation.service.SimulationAllocationPolicy;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.service.SimulationInput;
import com.jaedaero.domain.simulation.service.SimulationInputProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisHistoryServiceImpl implements AnalysisHistoryService {

  private static final int MAX_SUMMARY_LENGTH = 120;

  private final AnalysisHistoryMapper analysisHistoryMapper;
  private final SimulationInputProvider simulationInputProvider;
  private final SimulationCalculator simulationCalculator;
  private final SimulationAllocationPolicy allocationPolicy;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public AnalysisHistoryPageResponse getHistories(
      long userId, AnalysisHistoryFilter type, int page, int size) {
    long offset = Math.multiplyExact((long) page, size);
    long totalCount = analysisHistoryMapper.countByUserId(userId, type.name());
    List<AnalysisHistoryRow> rows =
        totalCount == 0
            ? List.of()
            : analysisHistoryMapper.findByUserId(userId, type.name(), offset, size);

    SimulationInput simulationInput =
        rows.stream().anyMatch(row -> row.getHistoryType() == AnalysisHistoryType.WHAT_IF)
            ? simulationInputProvider.load(userId)
            : null;
    List<AnalysisHistoryItemResponse> histories =
        rows.stream().map(row -> response(row, simulationInput)).toList();

    AnalysisHistoryRow latest =
        totalCount == 0 ? null : analysisHistoryMapper.findLatestByUserId(userId, type.name());
    return AnalysisHistoryPageResponse.builder()
        .histories(histories)
        .page(page)
        .size(size)
        .totalCount(totalCount)
        .hasNext(offset + histories.size() < totalCount)
        .latestAnalyzedAt(latest == null ? null : latest.getCreatedAt())
        .latestExpectedAsset(latest == null ? null : expectedAsset(latest, json(latest)))
        .build();
  }

  private AnalysisHistoryItemResponse response(
      AnalysisHistoryRow row, SimulationInput simulationInput) {
    if (row.getHistoryType() == AnalysisHistoryType.WHAT_IF) {
      return whatIfResponse(row, simulationInput);
    }
    return aiResponse(row);
  }

  private AnalysisHistoryItemResponse whatIfResponse(
      AnalysisHistoryRow row, SimulationInput simulationInput) {
    LocalDate calculationDate = row.getCreatedAt().toLocalDate();
    long referenceMonthlyIncome =
        simulationCalculator.referenceMonthlyIncome(simulationInput, calculationDate);
    SimulationAllocationMetrics metrics =
        allocationPolicy.metrics(
            row.getMonthlySpendingAmount(),
            row.getMonthlySavingAmount(),
            row.getMonthlyInvestmentAmount(),
            referenceMonthlyIncome);

    return AnalysisHistoryItemResponse.builder()
        .historyType(AnalysisHistoryType.WHAT_IF)
        .sourceId(row.getSourceId())
        .title(
            row.getScenarioName() == null || row.getScenarioName().isBlank()
                ? "What-if 시뮬레이션"
                : row.getScenarioName())
        .summary(
            String.format(
                Locale.KOREA,
                "월 소비 %,d원 · 군적금 %,d원 · 투자 %,d원 계획",
                row.getMonthlySpendingAmount(),
                row.getMonthlySavingAmount(),
                row.getMonthlyInvestmentAmount()))
        .createdAt(row.getCreatedAt())
        .expectedAsset(row.getExpectedAsset())
        .isApplied(Boolean.TRUE.equals(row.getApplied()))
        .monthlySpendingAmount(row.getMonthlySpendingAmount())
        .monthlySavingAmount(row.getMonthlySavingAmount())
        .monthlyInvestmentAmount(row.getMonthlyInvestmentAmount())
        .spendingRate(metrics.spendingRate())
        .savingRate(metrics.savingRate())
        .investmentRate(metrics.investmentRate())
        .build();
  }

  private AnalysisHistoryItemResponse aiResponse(AnalysisHistoryRow row) {
    JsonNode result = json(row);
    JsonNode spendingPattern = child(result, "spendingPattern");
    JsonNode expectedEffect = child(result, "spendingExpectedEffect");
    String comment = text(result, "comment");

    return AnalysisHistoryItemResponse.builder()
        .historyType(AnalysisHistoryType.AI)
        .sourceId(row.getSourceId())
        .title(aiTitle(row.getAnalysisType()))
        .summary(
            summary(
                comment,
                "거래 및 자산 흐름을 바탕으로 생성한 AI 분석입니다."))
        .createdAt(row.getCreatedAt())
        .expectedAsset(expectedAsset(row, result))
        .isApplied(Boolean.TRUE.equals(row.getApplied()))
        .spendingAmount(longValue(spendingPattern, "totalSpendingAmount"))
        .spendingChangeAmount(longValue(spendingPattern, "changeAmount"))
        .spendingChangeRate(decimalValue(spendingPattern, "changeRate"))
        .expectedAssetIncreaseAmount(longValue(expectedEffect, "expectedAssetIncreaseAmount"))
        .generationSource(row.getGenerationSource())
        .build();
  }

  private Long expectedAsset(AnalysisHistoryRow row, JsonNode result) {
    if (row.getHistoryType() == AnalysisHistoryType.WHAT_IF) {
      return row.getExpectedAsset();
    }
    Long currentExpectedAsset = longValue(result, "expectedAsset");
    if (currentExpectedAsset != null) {
      return currentExpectedAsset;
    }
    Long recommendedExpectedAsset =
        longValue(child(result, "recommendedScenario"), "expectedAsset");
    return recommendedExpectedAsset != null
        ? recommendedExpectedAsset
        : longValue(child(result, "spendingExpectedEffect"), "expectedAssetAfterImprovement");
  }

  private String aiTitle(AiAnalysisType analysisType) {
    if (analysisType == null) {
      return "AI 금융 분석";
    }
    return switch (analysisType) {
      case CONSUMPTION -> "AI 소비 분석";
      case SAVING -> "AI 저축 분석";
      case INVESTMENT -> "AI 투자 분석";
      case POLICY -> "AI 정책 분석";
      case DIAGNOSIS, SCENARIO_COMPARISON -> "AI 소비 분석";
    };
  }

  private String summary(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String singleLine = value.trim().replaceAll("\\s+", " ");
    return singleLine.length() <= MAX_SUMMARY_LENGTH
        ? singleLine
        : singleLine.substring(0, MAX_SUMMARY_LENGTH - 1) + "…";
  }

  private JsonNode json(AnalysisHistoryRow row) {
    if (row.getResultJson() == null || row.getResultJson().isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(row.getResultJson());
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      // 오래된 손상 행 하나 때문에 전체 이력 화면이 실패하지 않도록 카드 기본값을 사용한다.
      return null;
    }
  }

  private JsonNode child(JsonNode parent, String field) {
    if (parent == null || !parent.isObject()) {
      return null;
    }
    JsonNode value = parent.get(field);
    return value == null || value.isNull() ? null : value;
  }

  private String text(JsonNode parent, String field) {
    JsonNode value = child(parent, field);
    return value != null && value.isTextual() ? value.textValue() : null;
  }

  private Long longValue(JsonNode parent, String field) {
    JsonNode value = child(parent, field);
    return value != null && value.isIntegralNumber() ? value.longValue() : null;
  }

  private BigDecimal decimalValue(JsonNode parent, String field) {
    JsonNode value = child(parent, field);
    return value != null && value.isNumber() ? value.decimalValue() : null;
  }
}
