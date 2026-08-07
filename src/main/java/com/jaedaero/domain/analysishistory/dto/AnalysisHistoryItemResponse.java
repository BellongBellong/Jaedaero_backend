package com.jaedaero.domain.analysishistory.dto;

import com.jaedaero.domain.aianalysis.dto.AiGenerationSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisHistoryItemResponse {

  private final AnalysisHistoryType historyType;
  private final Long sourceId;
  private final String title;
  private final String summary;
  private final LocalDateTime createdAt;
  private final Long expectedAsset;
  private final Boolean isApplied;

  // What-if 카드 전용 필드
  private final Long monthlySpendingAmount;
  private final Long monthlySavingAmount;
  private final Long monthlyInvestmentAmount;
  private final BigDecimal spendingRate;
  private final BigDecimal savingRate;
  private final BigDecimal investmentRate;

  // AI 카드 전용 필드
  private final Long spendingAmount;
  private final Long spendingChangeAmount;
  private final BigDecimal spendingChangeRate;
  private final Long expectedAssetIncreaseAmount;
  private final AiGenerationSource generationSource;
}
