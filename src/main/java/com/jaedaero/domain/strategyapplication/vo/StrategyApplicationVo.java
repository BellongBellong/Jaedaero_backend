package com.jaedaero.domain.strategyapplication.vo;

import java.math.BigDecimal;
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
public class StrategyApplicationVo {

  private Long applicationId;
  private Long userId;
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
}
