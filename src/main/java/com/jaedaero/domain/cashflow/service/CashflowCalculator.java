package com.jaedaero.domain.cashflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    List<SavingMaturity> savings =
        savingsMaturingByDischarge(input.soldierSavings(), startMonth, dischargeMonth);
    boolean hasSoldierSavings = !savings.isEmpty();

    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      int remainingMonths = totalMonths - index;
      DefaultMilitaryPayPolicy.MilitaryPay pay =
          militaryPayPolicy.resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), month);
      long requiredSaving = requiredSaving(input.targetAmount(), asset, remainingMonths);
      long spending = input.monthlySpendingAverage();
      long monthlySaving =
          savings.stream()
              .filter(saving -> !month.isAfter(saving.maturityMonth()))
              .mapToLong(saving -> saving.input().monthlyAmount())
              .sum();
      long spendingLimit =
          Math.max(0L, pay.monthlySalary() - (hasSoldierSavings ? monthlySaving : requiredSaving));
      long maturityBonus =
          savings.stream()
              .filter(saving -> month.equals(saving.maturityMonth()))
              .mapToLong(SavingMaturity::bonus)
              .sum();

      asset += pay.monthlySalary() - spending + maturityBonus;
      expectedSalary += pay.monthlySalary();
      if (!hasSoldierSavings) expectedSavingAmount += requiredSaving;
      if (index == 0) firstMonthSpendingLimit = spendingLimit;
      if (financialDischargeDate == null && asset >= input.targetAmount()) {
        financialDischargeDate = month.atDay(1);
      }
      months.add(
          new CashflowForecastMonthCalculation(
              month.atDay(1),
              pay.rankName(),
              pay.monthlySalary(),
              hasSoldierSavings ? monthlySaving : requiredSaving,
              spending,
              asset));
    }

    if (hasSoldierSavings) {
      expectedSavingAmount = savings.stream().mapToLong(SavingMaturity::maturityPayout).sum();
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

  private List<SavingMaturity> savingsMaturingByDischarge(
      List<SoldierSavingInput> inputs, YearMonth startMonth, YearMonth dischargeMonth) {
    return inputs.stream()
        .filter(Objects::nonNull)
        .map(input -> toSavingMaturity(input, startMonth))
        .filter(
            saving ->
                !saving.maturityMonth().isBefore(startMonth)
                    && !saving.maturityMonth().isAfter(dischargeMonth))
        .toList();
  }

  private SavingMaturity toSavingMaturity(SoldierSavingInput input, YearMonth startMonth) {
    YearMonth maturityMonth = YearMonth.from(input.maturityDate());
    int depositMonths =
        maturityMonth.isBefore(startMonth)
            ? 0
            : (int) ChronoUnit.MONTHS.between(startMonth, maturityMonth) + 1;
    long futurePrincipal = Math.multiplyExact(input.monthlyAmount(), depositMonths);
    long principal = Math.addExact(input.currentBalance(), futurePrincipal);
    long interest = estimatedInterest(input, depositMonths);
    long governmentSupport = input.governmentSupportExpected();
    return new SavingMaturity(
        input, maturityMonth, principal + interest + governmentSupport, interest + governmentSupport);
  }

  private long estimatedInterest(SoldierSavingInput input, int depositMonths) {
    BigDecimal annualRate = input.annualInterestRate().movePointLeft(2);
    BigDecimal currentBalanceInterest =
        BigDecimal.valueOf(input.currentBalance())
            .multiply(annualRate)
            .multiply(BigDecimal.valueOf(depositMonths))
            .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
    long depositMonthWeights = (long) depositMonths * (depositMonths - 1) / 2;
    BigDecimal depositInterest =
        BigDecimal.valueOf(input.monthlyAmount())
            .multiply(annualRate)
            .multiply(BigDecimal.valueOf(depositMonthWeights))
            .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
    return currentBalanceInterest.add(depositInterest).longValueExact();
  }

  private double achievementRate(long expectedAsset, long targetAmount) {
    if (targetAmount == 0L) return 100D;
    return Math.min(999.99D, expectedAsset * 100D / targetAmount);
  }

  private record SavingMaturity(
      SoldierSavingInput input, YearMonth maturityMonth, long maturityPayout, long bonus) {}
}
