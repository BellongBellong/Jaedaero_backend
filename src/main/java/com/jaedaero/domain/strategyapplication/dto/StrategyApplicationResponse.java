package com.jaedaero.domain.strategyapplication.dto;

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
  private Long rebalancingId;
  private Long appliedMonthlySavingAmount;
  private BigDecimal appliedInvestmentRatio;
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
        .rebalancingId(source.getRebalancingId())
        .appliedMonthlySavingAmount(source.getAppliedMonthlySavingAmount())
        .appliedInvestmentRatio(source.getAppliedInvestmentRatio())
        .appliedExpectedReturnRate(source.getAppliedExpectedReturnRate())
        .appliedMonthlySpendingAmount(source.getAppliedMonthlySpendingAmount())
        .beforeExpectedAsset(source.getBeforeExpectedAsset())
        .afterExpectedAsset(source.getAfterExpectedAsset())
        .appliedAt(source.getAppliedAt())
        .build();
  }
}
