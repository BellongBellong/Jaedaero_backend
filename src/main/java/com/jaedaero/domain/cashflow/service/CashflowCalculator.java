package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Calculates the monthly salary, saving/investment allocation, spending, and ending asset forecast. */
@Component
public class CashflowCalculator {

  /** Initial allocation used until a synchronized soldier-savings amount is available. */
  public static final long DEFAULT_MONTHLY_SAVING_AMOUNT = 550_000L;
  public static final long DEFAULT_MONTHLY_SPENDING_AMOUNT = 0L;

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
    boolean hasAppliedStrategy = input.appliedStrategy() != null;

    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      int remainingMonths = totalMonths - index;
      DefaultMilitaryPayPolicy.MilitaryPay pay =
          militaryPayPolicy.resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), month);
      long requiredSaving = requiredSaving(input.targetAmount(), asset, remainingMonths);
      if (hasAppliedStrategy) {
        validateAppliedStrategy(input.appliedStrategy(), pay.monthlySalary());
      }
      long spending =
          hasAppliedStrategy
              ? input.appliedStrategy().monthlySpendingAmount()
              : Math.max(DEFAULT_MONTHLY_SPENDING_AMOUNT, input.monthlySpendingAverage());
      long monthlySaving =
          hasAppliedStrategy
              ? input.appliedStrategy().monthlySavingAmount()
              : hasSoldierSavings
              ? savings.stream()
                  .filter(saving -> !month.isAfter(saving.maturityMonth()))
                  .mapToLong(saving -> saving.input().monthlyAmount())
                  .sum()
              : Math.max(DEFAULT_MONTHLY_SAVING_AMOUNT, requiredSaving);
      long spendingLimit =
          hasAppliedStrategy
              ? spending
              : Math.max(0L, pay.monthlySalary() - monthlySaving);
      long monthlyInvestment =
          hasAppliedStrategy
              ? input.appliedStrategy().monthlyInvestmentAmount()
              : Math.min(
                  Math.max(0L, pay.monthlySalary() - spending - monthlySaving),
                  Math.max(0L, requiredSaving - monthlySaving));
      long maturityBonus =
          savings.stream()
              .filter(saving -> month.equals(saving.maturityMonth()))
              .mapToLong(SavingMaturity::bonus)
              .sum();

      long assetBeforeMonth = asset;
      asset += pay.monthlySalary() - spending + maturityBonus;
      expectedSalary += pay.monthlySalary();
      if (hasAppliedStrategy || !hasSoldierSavings) expectedSavingAmount += monthlySaving;
      if (index == 0) firstMonthSpendingLimit = spendingLimit;
      if (financialDischargeDate == null) {
        financialDischargeDate =
            estimatedFinancialDischargeDate(
                assetBeforeMonth, asset, input.targetAmount(), calculationDate, month);
      }
      months.add(
          new CashflowForecastMonthCalculation(
              month.atDay(1),
              pay.rankName(),
              pay.monthlySalary(),
              monthlySaving,
              monthlyInvestment,
              spending,
              asset));
    }

    if (hasSoldierSavings && !hasAppliedStrategy) {
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

  private void validateAppliedStrategy(
      AppliedCashflowStrategy strategy, long referenceMonthlyIncome) {
    if (strategy.monthlySpendingAmount() < 0
        || strategy.monthlySavingAmount() < 0
        || strategy.monthlySavingAmount() > DEFAULT_MONTHLY_SAVING_AMOUNT
        || strategy.monthlyInvestmentAmount() < 0
        || strategy.expectedReturnRate() == null
        || strategy.expectedReturnRate().signum() < 0) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 금액이 유효하지 않습니다.");
    }
    try {
      long allocated =
          Math.addExact(
              Math.addExact(
                  strategy.monthlySpendingAmount(), strategy.monthlySavingAmount()),
              strategy.monthlyInvestmentAmount());
      if (allocated > referenceMonthlyIncome) {
        throw new CashflowException(
            CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 합계가 해당 월 군 월급을 초과합니다.");
      }
    } catch (ArithmeticException exception) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 합계를 계산할 수 없습니다.");
    }
  }

  /**
   * Estimates the day within the month that the target is reached by spreading that month's net
   * asset increase evenly across the remaining calendar days. It is an estimate because salaries,
   * spending, and maturity benefits are currently forecast monthly.
   */
  private LocalDate estimatedFinancialDischargeDate(
      long assetBeforeMonth,
      long assetAfterMonth,
      long targetAmount,
      LocalDate calculationDate,
      YearMonth forecastMonth) {
    if (assetBeforeMonth >= targetAmount || assetAfterMonth < targetAmount) {
      return null;
    }
    long monthlyNetIncrease = assetAfterMonth - assetBeforeMonth;
    if (monthlyNetIncrease <= 0L) {
      return null;
    }

    LocalDate periodStart =
        forecastMonth.equals(YearMonth.from(calculationDate))
            ? calculationDate
            : forecastMonth.atDay(1);
    int remainingDays = (int) ChronoUnit.DAYS.between(periodStart, forecastMonth.atEndOfMonth()) + 1;
    long amountNeeded = targetAmount - assetBeforeMonth;
    long daysToReach = (amountNeeded * remainingDays + monthlyNetIncrease - 1) / monthlyNetIncrease;
    return periodStart.plusDays(daysToReach - 1);
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
