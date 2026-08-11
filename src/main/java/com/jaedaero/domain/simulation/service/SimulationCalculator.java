package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** 위키의 가정 시뮬레이션 월별 누적 산식을 구현합니다. */
@Component
public class SimulationCalculator {

  public static final BigDecimal SOLDIER_SAVING_ANNUAL_INTEREST_RATE = new BigDecimal("5.00");
  public static final BigDecimal GOVERNMENT_MATCHING_RATE = new BigDecimal("100.00");
  public static final String CALCULATION_POLICY_VERSION = "WHAT_IF_DETAIL_V1_20260806";

  private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
  private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
  private static final MathContext RETURN_MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

  private final DefaultMilitaryPayPolicy militaryPayPolicy;

  public SimulationCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this.militaryPayPolicy = militaryPayPolicy;
  }

  public long referenceMonthlyIncome(SimulationInput input, LocalDate calculationDate) {
    return militaryPayPolicy
        .resolve(
            input.soldierType(),
            YearMonth.from(input.enlistmentDate()),
            YearMonth.from(calculationDate))
        .monthlySalary();
  }

  public SimulationCalculationResult calculate(
      SimulationInput input, SimulationRequest request, LocalDate calculationDate) {
    long asset = input.baseAsset();

    YearMonth startMonth = YearMonth.from(calculationDate);
    YearMonth dischargeMonth = YearMonth.from(input.dischargeDate());
    if (startMonth.isAfter(dischargeMonth)) {
      return emptyResult(input, asset >= input.targetAmount() ? calculationDate : null);
    }

    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
    YearMonth enlistmentMonth = YearMonth.from(input.enlistmentDate());
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    long expectedSalary = 0L;
    long expectedSpending = 0L;
    long unallocatedPrincipal = 0L;
    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      long salary =
          militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, month).monthlySalary();
      long assetBeforeMonth = asset;

      // 가정 시뮬레이션의 저축액과 투자액은 순자산 내부 배분입니다. MVP 예상자산은
      // 캐시플로우 계약과 동일하게 급여 - 소비만 순증가로 반영한다.
      asset = Math.addExact(asset, Math.subtractExact(salary, request.getMonthlySpendingAmount()));
      expectedSalary = Math.addExact(expectedSalary, salary);
      expectedSpending = Math.addExact(expectedSpending, request.getMonthlySpendingAmount());
      unallocatedPrincipal =
          Math.addExact(
              unallocatedPrincipal,
              Math.subtractExact(
                  Math.subtractExact(
                      Math.subtractExact(salary, request.getMonthlySpendingAmount()),
                      request.getMonthlySavingAmount()),
                  request.getMonthlyInvestmentAmount()));
      if (financialDischargeDate == null) {
        financialDischargeDate =
            estimatedFinancialDischargeDate(
                assetBeforeMonth,
                asset,
                input.targetAmount(),
                calculationDate,
                input.dischargeDate(),
                month);
      }
    }

    long soldierSavingPrincipal =
        Math.multiplyExact(request.getMonthlySavingAmount(), totalMonths);
    long soldierSavingInterest =
        compoundReturn(
            request.getMonthlySavingAmount(),
            SOLDIER_SAVING_ANNUAL_INTEREST_RATE,
            totalMonths);
    long governmentMatchingSupport =
        rateAmount(soldierSavingPrincipal, GOVERNMENT_MATCHING_RATE);
    long investmentPrincipal =
        Math.multiplyExact(request.getMonthlyInvestmentAmount(), totalMonths);
    long expectedInvestmentReturn =
        compoundReturn(
            request.getMonthlyInvestmentAmount(), request.getExpectedReturnRate(), totalMonths);
    long projectedBenefit =
        Math.addExact(
            Math.addExact(soldierSavingInterest, governmentMatchingSupport),
            expectedInvestmentReturn);
    long potentialExpectedAsset = Math.addExact(asset, projectedBenefit);

    return new SimulationCalculationResult(
        asset,
        financialDischargeDate,
        totalMonths,
        input.baseAsset(),
        expectedSalary,
        expectedSpending,
        soldierSavingPrincipal,
        soldierSavingInterest,
        governmentMatchingSupport,
        investmentPrincipal,
        expectedInvestmentReturn,
        unallocatedPrincipal,
        potentialExpectedAsset,
        CALCULATION_POLICY_VERSION);
  }

  private SimulationCalculationResult emptyResult(
      SimulationInput input, LocalDate financialDischargeDate) {
    return new SimulationCalculationResult(
        input.baseAsset(),
        financialDischargeDate,
        0,
        input.baseAsset(),
        0L,
        0L,
        0L,
        0L,
        0L,
        0L,
        0L,
        0L,
        input.baseAsset(),
        CALCULATION_POLICY_VERSION);
  }

  private long compoundReturn(
      long monthlyContribution, BigDecimal annualRatePercent, int months) {
    if (monthlyContribution == 0L || annualRatePercent.signum() == 0 || months <= 1) {
      return 0L;
    }
    BigDecimal monthlyRate =
        annualRatePercent.divide(PERCENT, RETURN_MATH_CONTEXT)
            .divide(MONTHS_PER_YEAR, RETURN_MATH_CONTEXT);
    BigDecimal balance = BigDecimal.ZERO;
    BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate);
    for (int month = 0; month < months; month++) {
      balance =
          balance.multiply(growthFactor, RETURN_MATH_CONTEXT)
              .add(BigDecimal.valueOf(monthlyContribution));
    }
    long principal = Math.multiplyExact(monthlyContribution, months);
    return balance.subtract(BigDecimal.valueOf(principal)).setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private long rateAmount(long amount, BigDecimal ratePercent) {
    return BigDecimal.valueOf(amount)
        .multiply(ratePercent)
        .divide(PERCENT, 0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private LocalDate estimatedFinancialDischargeDate(
      long assetBeforeMonth,
      long assetAfterMonth,
      long targetAmount,
      LocalDate calculationDate,
      LocalDate dischargeDate,
      YearMonth forecastMonth) {
    if (assetBeforeMonth >= targetAmount || assetAfterMonth < targetAmount) {
      return null;
    }
    long monthlyNetIncrease = assetAfterMonth - assetBeforeMonth;
    if (monthlyNetIncrease <= 0) {
      return null;
    }

    LocalDate periodStart =
        forecastMonth.equals(YearMonth.from(calculationDate))
            ? calculationDate
            : forecastMonth.atDay(1);
    LocalDate periodEnd =
        forecastMonth.equals(YearMonth.from(dischargeDate))
            ? dischargeDate
            : forecastMonth.atEndOfMonth();
    int periodDays = (int) ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
    long amountNeeded = targetAmount - assetBeforeMonth;
    long daysToReach =
        (Math.multiplyExact(amountNeeded, periodDays) + monthlyNetIncrease - 1)
            / monthlyNetIncrease;
    return periodStart.plusDays(daysToReach - 1);
  }
}
