package com.jaedaero.domain.strategyapplication.dto;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationSourceType;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyApplicationResponse {

  private Long applicationId;
  private StrategyApplicationSourceType sourceType;
  private Long simulationId;
  private Long aiScenarioId;
  private Long guidanceId;
  private InvestmentGuidanceAction appliedGuidanceAction;
  private InvestmentFrequency appliedInvestmentFrequency;
  private Long appliedRecurringContributionAmount;
  private Long appliedMonthlySavingAmount;
  private Long appliedMonthlyInvestmentAmount;
  private BigDecimal appliedExpectedReturnRate;
  private Long appliedMonthlySpendingAmount;
  private Long beforeExpectedAsset;
  private Long afterExpectedAsset;
  private LocalDateTime appliedAt;

  public static StrategyApplicationResponse from(StrategyApplicationVo source) {
    return builder()
        .applicationId(source.getApplicationId())
        .sourceType(source.getSourceType())
        .simulationId(source.getSimulationId())
        .aiScenarioId(source.getAiScenarioId())
        .guidanceId(source.getGuidanceId())
        .appliedGuidanceAction(source.getAppliedGuidanceAction())
        .appliedInvestmentFrequency(source.getAppliedInvestmentFrequency())
        .appliedRecurringContributionAmount(source.getAppliedRecurringContributionAmount())
        .appliedMonthlySavingAmount(source.getAppliedMonthlySavingAmount())
        .appliedMonthlyInvestmentAmount(source.getAppliedMonthlyInvestmentAmount())
        .appliedExpectedReturnRate(source.getAppliedExpectedReturnRate())
        .appliedMonthlySpendingAmount(source.getAppliedMonthlySpendingAmount())
        .beforeExpectedAsset(source.getBeforeExpectedAsset())
        .afterExpectedAsset(source.getAfterExpectedAsset())
        .appliedAt(source.getAppliedAt())
        .build();
  }
}
