package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Calculates the monthly salary, savings target, spending limit, and ending asset forecast. */
@Component
public class CashflowCalculator {

  private final DefaultMilitaryPayPolicy militaryPayPolicy;

  public CashflowCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this.militaryPayPolicy = militaryPayPolicy;
  }

  public CashflowForecastCalculation calculate(CashflowInput input, LocalDate calculationDate) {
    YearMonth startMonth = YearMonth.from(calculationDate);
    YearMonth dischargeMonth = YearMonth.from(input.dischargeDate());
    if (startMonth.isAfter(dischargeMonth)) {
      return new CashflowForecastCalculation(
          0L, 0L, input.baseAsset(), 0L, achievementRate(input.baseAsset(), input.targetAmount()), null, List.of());
    }

    long asset = input.baseAsset();
    long expectedSalary = 0L;
    long expectedSavingAmount = 0L;
    long firstMonthSpendingLimit = 0L;
    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
    List<CashflowForecastMonthCalculation> months = new ArrayList<>();
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;

    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      int remainingMonths = totalMonths - index;
      DefaultMilitaryPayPolicy.MilitaryPay pay =
          militaryPayPolicy.resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), month);
      long requiredSaving = requiredSaving(input.targetAmount(), asset, remainingMonths);
      long spendingLimit = Math.max(0L, pay.monthlySalary() - requiredSaving);
      long spending = input.monthlySpendingAverage();

      asset += pay.monthlySalary() - spending;
      expectedSalary += pay.monthlySalary();
      expectedSavingAmount += requiredSaving;
      if (index == 0) firstMonthSpendingLimit = spendingLimit;
      if (financialDischargeDate == null && asset >= input.targetAmount()) {
        financialDischargeDate = month.atDay(1);
      }
      months.add(
          new CashflowForecastMonthCalculation(
              month.atDay(1),
              pay.rankName(),
              pay.monthlySalary(),
              requiredSaving,
              spending,
              asset));
    }

    return new CashflowForecastCalculation(
        expectedSalary,
        expectedSavingAmount,
        asset,
        firstMonthSpendingLimit,
        achievementRate(asset, input.targetAmount()),
        financialDischargeDate,
        List.copyOf(months));
  }

  private long requiredSaving(long targetAmount, long asset, int remainingMonths) {
    long remainingAmount = Math.max(0L, targetAmount - asset);
    return (remainingAmount + remainingMonths - 1) / remainingMonths;
  }

  private double achievementRate(long expectedAsset, long targetAmount) {
    if (targetAmount == 0L) return 100D;
    return Math.min(999.99D, expectedAsset * 100D / targetAmount);
  }
}
