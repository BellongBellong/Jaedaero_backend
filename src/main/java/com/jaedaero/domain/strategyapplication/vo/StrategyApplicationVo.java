package com.jaedaero.domain.strategyapplication.vo;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
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
}
