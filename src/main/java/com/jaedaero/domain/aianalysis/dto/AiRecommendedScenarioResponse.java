package com.jaedaero.domain.aianalysis.dto;

import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class AiRecommendedScenarioResponse {
  private Long scenarioId;
  private Long monthlySavingAmount;
  private BigDecimal investmentRatio;
  private BigDecimal expectedReturnRate;
  private Long monthlySpendingAmount;
  private Long expectedAsset;
  private LocalDate financialDischargeDate;
  private String recommendReason;

  public static AiRecommendedScenarioResponse from(AiRecommendedScenarioVo source) {
    return builder().scenarioId(source.getScenarioId()).monthlySavingAmount(source.getMonthlySavingAmount())
        .investmentRatio(source.getInvestmentRatio()).expectedReturnRate(source.getExpectedReturnRate())
        .monthlySpendingAmount(source.getMonthlySpendingAmount()).expectedAsset(source.getExpectedAsset())
        .financialDischargeDate(source.getFinancialDischargeDate()).recommendReason(source.getRecommendReason()).build();
  }
}
