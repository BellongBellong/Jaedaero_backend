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

/** 월별 급여, 저축·투자 배분, 지출 및 기말 자산 예측을 계산합니다. */
@Component
public class CashflowCalculator {

  /** 동기화된 장병 적금 금액을 가져오기 전까지 사용할 초기 배분값입니다. */
  public static final long DEFAULT_MONTHLY_SAVING_AMOUNT = 550_000L;
  public static final long DEFAULT_MONTHLY_SPENDING_AMOUNT = 0L;
  private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
  private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

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
    long expectedInvestmentReturn = 0L;
    long firstMonthSpendingLimit = 0L;
    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
    List<CashflowForecastMonthCalculation> months = new ArrayList<>();
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    boolean hasAppliedStrategy = input.appliedStrategy() != null;
    List<SavingMaturity> savings =
        savingsMaturingByDischarge(
            input.soldierSavings(),
            startMonth,
            dischargeMonth,
            hasAppliedStrategy ? input.appliedStrategy().monthlySavingAmount() : null);
    boolean hasSoldierSavings = !savings.isEmpty();

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
              : DEFAULT_MONTHLY_SPENDING_AMOUNT;
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
              : 0L;
      long maturityBonus =
          savings.stream()
              .filter(saving -> month.equals(saving.maturityMonth()))
              .mapToLong(SavingMaturity::bonus)
              .sum();
      long cumulativeInvestmentReturn =
          hasAppliedStrategy
              ? compoundReturn(
                  monthlyInvestment, input.appliedStrategy().expectedReturnRate(), index + 1)
              : 0L;
      long monthlyInvestmentReturn = cumulativeInvestmentReturn - expectedInvestmentReturn;

      long assetBeforeMonth = asset;
      asset += pay.monthlySalary() - spending + maturityBonus + monthlyInvestmentReturn;
      expectedInvestmentReturn = cumulativeInvestmentReturn;
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
   * 해당 월의 순자산 증가액을 남은 날짜에 균등하게 배분해 목표 달성일을 추정합니다. 급여·지출·만기
   * 혜택이 현재 월 단위로 예측되므로 이 값은 추정치입니다.
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
      List<SoldierSavingInput> inputs,
      YearMonth startMonth,
      YearMonth dischargeMonth,
      Long monthlySavingAmountOverride) {
    List<SoldierSavingInput> validInputs =
        inputs.stream()
        .filter(Objects::nonNull)
        .toList();
    long configuredMonthlyAmount =
        validInputs.stream().mapToLong(SoldierSavingInput::monthlyAmount).sum();
    List<SavingMaturity> savings = new ArrayList<>();
    long remainingOverride = monthlySavingAmountOverride == null ? 0L : monthlySavingAmountOverride;
    for (int index = 0; index < validInputs.size(); index++) {
      SoldierSavingInput input = validInputs.get(index);
      long monthlyAmount =
          monthlySavingAmountOverride == null
              ? input.monthlyAmount()
              : allocatedMonthlySavingAmount(
                  input, index, validInputs.size(), configuredMonthlyAmount, remainingOverride, monthlySavingAmountOverride);
      if (monthlySavingAmountOverride != null) {
        remainingOverride -= monthlyAmount;
      }
      SavingMaturity saving = toSavingMaturity(input, startMonth, monthlyAmount);
      if (!saving.maturityMonth().isBefore(startMonth) && !saving.maturityMonth().isAfter(dischargeMonth)) {
        savings.add(saving);
      }
    }
    return List.copyOf(savings);
  }

  private long allocatedMonthlySavingAmount(
      SoldierSavingInput input,
      int index,
      int totalCount,
      long configuredMonthlyAmount,
      long remainingOverride,
      long monthlySavingAmountOverride) {
    if (index == totalCount - 1) {
      return remainingOverride;
    }
    if (configuredMonthlyAmount == 0L) {
      return monthlySavingAmountOverride / totalCount;
    }
    return BigDecimal.valueOf(monthlySavingAmountOverride)
        .multiply(BigDecimal.valueOf(input.monthlyAmount()))
        .divide(BigDecimal.valueOf(configuredMonthlyAmount), 0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private SavingMaturity toSavingMaturity(
      SoldierSavingInput input, YearMonth startMonth, long monthlyAmount) {
    YearMonth maturityMonth = YearMonth.from(input.maturityDate());
    int depositMonths =
        maturityMonth.isBefore(startMonth)
            ? 0
            : (int) ChronoUnit.MONTHS.between(startMonth, maturityMonth) + 1;
    long futurePrincipal = Math.multiplyExact(monthlyAmount, depositMonths);
    long principal = Math.addExact(input.currentBalance(), futurePrincipal);
    long interest = estimatedInterest(input, monthlyAmount, depositMonths);
    // 장병내일준비적금은 만기 해지 시 납입 원금의 100%를 매칭지원금으로 지급한다.
    // 현재 잔액은 이미 납입한 원금으로, 미래 납입액까지 합산해 지원금을 계산한다.
    long governmentSupport = principal;
    return new SavingMaturity(
        input, maturityMonth, principal + interest + governmentSupport, interest + governmentSupport);
  }

  private long estimatedInterest(SoldierSavingInput input, long monthlyAmount, int depositMonths) {
    BigDecimal annualRate = input.annualInterestRate().movePointLeft(2);
    BigDecimal currentBalanceInterest =
        BigDecimal.valueOf(input.currentBalance())
            .multiply(annualRate)
            .multiply(BigDecimal.valueOf(depositMonths))
            .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
    long depositMonthWeights = (long) depositMonths * (depositMonths - 1) / 2;
    BigDecimal depositInterest =
        BigDecimal.valueOf(monthlyAmount)
            .multiply(annualRate)
            .multiply(BigDecimal.valueOf(depositMonthWeights))
            .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
    return currentBalanceInterest.add(depositInterest).longValueExact();
  }

  private long compoundReturn(long monthlyContribution, BigDecimal annualRatePercent, int months) {
    if (monthlyContribution == 0L || annualRatePercent.signum() == 0 || months <= 1) {
      return 0L;
    }
    BigDecimal monthlyRate =
        annualRatePercent
            .divide(PERCENT, 20, RoundingMode.HALF_UP)
            .divide(MONTHS_PER_YEAR, 20, RoundingMode.HALF_UP);
    BigDecimal balance = BigDecimal.ZERO;
    BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate);
    for (int month = 0; month < months; month++) {
      balance =
          balance.multiply(growthFactor).add(BigDecimal.valueOf(monthlyContribution));
    }
    long principal = Math.multiplyExact(monthlyContribution, months);
    return balance.subtract(BigDecimal.valueOf(principal)).setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private double achievementRate(long expectedAsset, long targetAmount) {
    if (targetAmount == 0L) return 100D;
    return Math.min(999.99D, expectedAsset * 100D / targetAmount);
  }

  private record SavingMaturity(
      SoldierSavingInput input, YearMonth maturityMonth, long maturityPayout, long bonus) {}
}
