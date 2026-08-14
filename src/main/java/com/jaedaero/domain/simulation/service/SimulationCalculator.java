package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngine;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.cashflow.service.AggregateInvestmentPrincipalProvider;
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
  public static final String CALCULATION_POLICY_VERSION = "WHAT_IF_DETAIL_V3_20260813";

  private final DefaultMilitaryPayPolicy militaryPayPolicy;
  private final ConservativeMonthlyCashflowEngine cashflowEngine;
  private final AggregateInvestmentPrincipalProvider investmentPrincipalProvider;
  private final InvestmentPrincipalBackfill investmentPrincipalBackfill;

  @Autowired
  public SimulationCalculator(
      DefaultMilitaryPayPolicy militaryPayPolicy,
      ConservativeMonthlyCashflowEngine cashflowEngine,
      AggregateInvestmentPrincipalProvider investmentPrincipalProvider,
      InvestmentPrincipalBackfill investmentPrincipalBackfill) {
    this.militaryPayPolicy = militaryPayPolicy;
    this.cashflowEngine = cashflowEngine;
    this.investmentPrincipalProvider = investmentPrincipalProvider;
    this.investmentPrincipalBackfill = investmentPrincipalBackfill;
  }

  /** 단위 테스트와 독립 계산 호출의 하위 호환용 생성자입니다. */
  public SimulationCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this(
        militaryPayPolicy,
        new ConservativeMonthlyCashflowEngine(),
        userId -> java.util.Optional.empty(),
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

    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
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
              spending,
              input.targetAmount(),
              calculationDate,
              input.dischargeDate(),
              month);
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
      savingContributionDates.add(month.atDay(1));
      investmentContributions.add(investment);
      if (financialDischargeDate == null && projection.targetReachedDate() != null) {
        financialDischargeDate = projection.targetReachedDate();
      }
    }

    BigDecimal finalInvestmentRatio = investmentRatio;
    long existingInvestmentPrincipal =
        investmentPrincipalProvider
            .resolveLinkedPrincipal(input.userId())
            .orElseGet(
                () ->
                    investmentPrincipalBackfill.estimate(
                        input.soldierType(),
                        input.enlistmentDate(),
                        calculationDate,
                        finalInvestmentRatio));
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        cashflowEngine.calculateProjectedBenefit(
            input.soldierSavings(),
            calculationDate,
            savingContributions,
            savingContributionDates,
            input.dischargeDate(),
            existingInvestmentPrincipal,
            investmentContributions,
            request.getExpectedReturnRate());
    long expectedAsset = Math.addExact(asset, benefit.projectedBenefitAmount());
    if (financialDischargeDate == null && expectedAsset >= input.targetAmount()) {
      financialDischargeDate = input.dischargeDate();
    }

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
