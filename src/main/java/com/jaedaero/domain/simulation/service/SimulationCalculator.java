package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngine;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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
  public static final String CALCULATION_POLICY_VERSION = "WHAT_IF_DETAIL_V2_20260813";

  private final DefaultMilitaryPayPolicy militaryPayPolicy;
  private final ConservativeMonthlyCashflowEngine cashflowEngine;

  @Autowired
  public SimulationCalculator(
      DefaultMilitaryPayPolicy militaryPayPolicy,
      ConservativeMonthlyCashflowEngine cashflowEngine) {
    this.militaryPayPolicy = militaryPayPolicy;
    this.cashflowEngine = cashflowEngine;
  }

  /** 단위 테스트와 독립 계산 호출의 하위 호환용 생성자입니다. */
  public SimulationCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this(militaryPayPolicy, new ConservativeMonthlyCashflowEngine());
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
    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      long salary =
          militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, month).monthlySalary();
      long assetBeforeMonth = asset;

      ConservativeMonthlyCashflowEngine.MonthProjection projection =
          cashflowEngine.project(
              assetBeforeMonth,
              salary,
              request.getMonthlySpendingAmount(),
              input.targetAmount(),
              calculationDate,
              input.dischargeDate(),
              month);
      asset = projection.endingAsset();
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
      if (financialDischargeDate == null && projection.targetReachedDate() != null) {
        financialDischargeDate = projection.targetReachedDate();
      }
    }

    List<Long> savingContributions =
        Collections.nCopies(totalMonths, request.getMonthlySavingAmount());
    List<Long> investmentContributions =
        Collections.nCopies(totalMonths, request.getMonthlyInvestmentAmount());
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        cashflowEngine.calculateProjectedBenefit(
            savingContributions, investmentContributions, request.getExpectedReturnRate());
    long potentialExpectedAsset = Math.addExact(asset, benefit.projectedBenefitAmount());

    return new SimulationCalculationResult(
        asset,
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
}
