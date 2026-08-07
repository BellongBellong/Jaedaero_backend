package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.service.SimulationAllocationMetrics;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** What-if 화면 최초 진입 시 사용할 서버 계산 기본값이다. */
@Getter
@Builder
public class SimulationDefaultsResponse {

  private final Long targetAmount;
  private final Long referenceMonthlyIncome;
  private final Long monthlySpendingAmount;
  private final Long monthlySavingAmount;
  private final Long monthlyInvestmentAmount;
  private final BigDecimal expectedReturnRate;
  private final Long maxMonthlySavingAmount;
  private final BigDecimal spendingRate;
  private final BigDecimal savingRate;
  private final BigDecimal investmentRate;
  private final Long unallocatedAmount;

  public static SimulationDefaultsResponse of(
      long targetAmount,
      long monthlySpendingAmount,
      long monthlySavingAmount,
      long monthlyInvestmentAmount,
      BigDecimal expectedReturnRate,
      long maxMonthlySavingAmount,
      SimulationAllocationMetrics metrics) {
    return builder()
        .targetAmount(targetAmount)
        .referenceMonthlyIncome(metrics.referenceMonthlyIncome())
        .monthlySpendingAmount(monthlySpendingAmount)
        .monthlySavingAmount(monthlySavingAmount)
        .monthlyInvestmentAmount(monthlyInvestmentAmount)
        .expectedReturnRate(expectedReturnRate)
        .maxMonthlySavingAmount(maxMonthlySavingAmount)
        .spendingRate(metrics.spendingRate())
        .savingRate(metrics.savingRate())
        .investmentRate(metrics.investmentRate())
        .unallocatedAmount(metrics.unallocatedAmount())
        .build();
  }
}
