package com.jaedaero.domain.aianalysis.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendedScenarioVo {

  private Long scenarioId;
  private Long userId;
  private Long monthlySavingAmount;
  private Long monthlyInvestmentAmount;
  private BigDecimal expectedReturnRate;
  private Long monthlySpendingAmount;
  private Long expectedAsset;
  private LocalDate financialDischargeDate;
  private String recommendReason;
  private LocalDateTime createdAt;
}
