package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngine;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.cashflow.service.InvestmentPrincipalBackfill;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 위키의 가정 시뮬레이션 월별 누적 산식을 구현합니다. */
@Component
public class SimulationCalculator {

  public static final BigDecimal SOLDIER_SAVING_ANNUAL_INTEREST_RATE =
      ConservativeMonthlyCashflowEngine.SOLDIER_SAVING_ANNUAL_INTEREST_RATE;
  public static final BigDecimal GOVERNMENT_MATCHING_RATE =
      ConservativeMonthlyCashflowEngine.GOVERNMENT_MATCHING_RATE;
  public static final String CALCULATION_POLICY_VERSION = "WHAT_IF_UNIFIED_ASSET_TIMELINE_V4_20260814";

  private final DefaultMilitaryPayPolicy militaryPayPolicy;
  private final ConservativeMonthlyCashflowEngine cashflowEngine;
  private final InvestmentPrincipalBackfill investmentPrincipalBackfill;

  @Autowired
  public SimulationCalculator(
      DefaultMilitaryPayPolicy militaryPayPolicy,
      ConservativeMonthlyCashflowEngine cashflowEngine,
      InvestmentPrincipalBackfill investmentPrincipalBackfill) {
    this.militaryPayPolicy = militaryPayPolicy;
    this.cashflowEngine = cashflowEngine;
    this.investmentPrincipalBackfill = investmentPrincipalBackfill;
  }

  /** 단위 테스트와 독립 계산 호출의 하위 호환용 생성자입니다. */
  public SimulationCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this(
        militaryPayPolicy,
        new ConservativeMonthlyCashflowEngine(),
        new InvestmentPrincipalBackfill(militaryPayPolicy));
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
    if (calculationDate.isAfter(input.dischargeDate())) {
      return emptyResult(input, asset >= input.targetAmount() ? calculationDate : null);
    }

    YearMonth enlistmentMonth = YearMonth.from(input.enlistmentDate());
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    long expectedSalary = 0L;
    long expectedSpending = 0L;
    long unallocatedPrincipal = 0L;
    List<Long> savingContributions = new ArrayList<>();
    List<LocalDate> savingContributionDates = new ArrayList<>();
    List<Long> investmentContributions = new ArrayList<>();

    long firstMonthSalary =
        militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, startMonth).monthlySalary();
    BigDecimal spendingRatio =
        firstMonthSalary > 0L
            ? BigDecimal.valueOf(request.getMonthlySpendingAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    BigDecimal investmentRatio =
        firstMonthSalary > 0L
            ? BigDecimal.valueOf(request.getMonthlyInvestmentAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    BigDecimal finalInvestmentRatio = investmentRatio;
    long existingInvestmentPrincipal =
        investmentPrincipalBackfill.estimate(
            input.soldierType(),
            input.enlistmentDate(),
            calculationDate,
            finalInvestmentRatio);
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        cashflowEngine.calculateProjectedBenefit(
            input.soldierSavings(),
            calculationDate,
            List.of(),
            List.of(),
            calculationDate,
            existingInvestmentPrincipal,
            List.of(),
            request.getExpectedReturnRate());
    long openingUnifiedAsset = cashflowEngine.unifiedAsset(asset, benefit);
    LocalDate financialDischargeDate =
        openingUnifiedAsset >= input.targetAmount() ? calculationDate : null;
    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      long salary =
          militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, month).monthlySalary();
      long spending = scale(spendingRatio, salary);
      long investment = scale(investmentRatio, salary);
      long assetBeforeMonth = asset;

      ConservativeMonthlyCashflowEngine.MonthProjection projection =
          cashflowEngine.project(
              assetBeforeMonth,
              salary,
              spending);
      asset = projection.endingAsset();
      expectedSalary = Math.addExact(expectedSalary, salary);
      expectedSpending = Math.addExact(expectedSpending, spending);
      unallocatedPrincipal =
          Math.addExact(
              unallocatedPrincipal,
              Math.subtractExact(
                  Math.subtractExact(
                      Math.subtractExact(salary, spending),
                      request.getMonthlySavingAmount()),
                  investment));
      savingContributions.add(request.getMonthlySavingAmount());
      savingContributionDates.add(
          month.equals(startMonth) ? calculationDate : month.atDay(1));
      investmentContributions.add(investment);
      LocalDate valuationDate =
          month.equals(dischargeMonth) ? input.dischargeDate() : month.atEndOfMonth();
      benefit =
          cashflowEngine.calculateProjectedBenefit(
              input.soldierSavings(),
              calculationDate,
              savingContributions,
              savingContributionDates,
              valuationDate,
              existingInvestmentPrincipal,
              investmentContributions,
              request.getExpectedReturnRate());
      long endingUnifiedAsset = cashflowEngine.unifiedAsset(asset, benefit);
      if (financialDischargeDate == null) {
        financialDischargeDate =
            cashflowEngine.estimateTargetReachedDate(
                openingUnifiedAsset,
                endingUnifiedAsset,
                input.targetAmount(),
                calculationDate,
                input.dischargeDate(),
                month);
      }
      openingUnifiedAsset = endingUnifiedAsset;
    }

    long expectedAsset = openingUnifiedAsset;

    return new SimulationCalculationResult(
        expectedAsset,
        financialDischargeDate,
        totalMonths,
        input.baseAsset(),
        expectedSalary,
        expectedSpending,
        benefit.soldierSavingPrincipal(),
        benefit.soldierSavingInterest(),
        benefit.governmentMatchingSupport(),
        benefit.investmentPrincipal(),
        benefit.expectedInvestmentReturn(),
        unallocatedPrincipal,
        CALCULATION_POLICY_VERSION);
  }

  private long scale(BigDecimal ratio, long referenceSalary) {
    return ratio
        .multiply(BigDecimal.valueOf(referenceSalary))
        .setScale(0, java.math.RoundingMode.HALF_UP)
        .longValueExact();
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
        CALCULATION_POLICY_VERSION);
  }
}
