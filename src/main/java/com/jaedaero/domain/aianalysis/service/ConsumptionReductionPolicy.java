package com.jaedaero.domain.aianalysis.service;

import org.springframework.stereotype.Component;

/** 군 생활에 필요한 최소 소비를 보장하면서 실행 가능한 월 소비 절감액을 산정합니다. */
@Component
public class ConsumptionReductionPolicy {

  static final long MINIMUM_MONTHLY_SPENDING_AMOUNT = 100_000L;
  static final long RECOMMENDATION_UNIT = 10_000L;
  static final int MINIMUM_INCOME_RATE_PERCENT = 10;
  static final int MAXIMUM_REDUCTION_RATE_PERCENT = 30;

  public long recommendReduction(
      long currentPlannedSpending, long observedIncrease, long referenceMonthlyIncome) {
    if (currentPlannedSpending <= 0 || observedIncrease <= 0) {
      return 0L;
    }

    long incomeBasedFloor =
        roundUp(referenceMonthlyIncome * MINIMUM_INCOME_RATE_PERCENT / 100L);
    long spendingFloor = Math.max(MINIMUM_MONTHLY_SPENDING_AMOUNT, incomeBasedFloor);
    long reducibleAmount = Math.max(0L, currentPlannedSpending - spendingFloor);
    long maximumReduction =
        currentPlannedSpending * MAXIMUM_REDUCTION_RATE_PERCENT / 100L;
    long candidate = Math.min(observedIncrease, Math.min(reducibleAmount, maximumReduction));
    return roundDown(candidate);
  }

  private long roundUp(long amount) {
    if (amount <= 0) {
      return 0L;
    }
    return ((amount + RECOMMENDATION_UNIT - 1) / RECOMMENDATION_UNIT)
        * RECOMMENDATION_UNIT;
  }

  private long roundDown(long amount) {
    return Math.max(0L, amount / RECOMMENDATION_UNIT * RECOMMENDATION_UNIT);
  }
}
