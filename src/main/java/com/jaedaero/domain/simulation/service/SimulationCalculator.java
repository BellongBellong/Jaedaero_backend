package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** 위키의 What-if 월별 누적 산식을 구현한다. */
@Component
public class SimulationCalculator {

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
    if (asset >= input.targetAmount()) {
      return new SimulationCalculationResult(asset, calculationDate);
    }

    YearMonth startMonth = YearMonth.from(calculationDate);
    YearMonth dischargeMonth = YearMonth.from(input.dischargeDate());
    if (startMonth.isAfter(dischargeMonth)) {
      return new SimulationCalculationResult(asset, null);
    }

    LocalDate financialDischargeDate = null;
    YearMonth enlistmentMonth = YearMonth.from(input.enlistmentDate());
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      long salary =
          militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, month).monthlySalary();
      long assetBeforeMonth = asset;

      // What-if의 저축액과 투자액은 순자산 내부 배분이다. MVP 예상자산은
      // 캐시플로우 계약과 동일하게 급여 - 소비만 순증가로 반영한다.
      asset = Math.addExact(asset, Math.subtractExact(salary, request.getMonthlySpendingAmount()));
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

    return new SimulationCalculationResult(asset, financialDischargeDate);
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
