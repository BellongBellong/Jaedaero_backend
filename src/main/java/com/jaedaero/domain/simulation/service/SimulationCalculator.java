package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.simulation.dto.SimulationRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** 위키의 What-if 월별 누적 산식을 구현한다. */
@Component
public class SimulationCalculator {

  public SimulationCalculationResult calculate(
      SimulationInput input, SimulationRequest request, LocalDate calculationDate) {
    long asset = input.baseAsset();
    if (asset >= input.targetAmount()) {
      return new SimulationCalculationResult(asset, calculationDate);
    }

    int remainingMonths = remainingMonths(calculationDate, input.dischargeDate());
    if (remainingMonths == 0) {
      return new SimulationCalculationResult(asset, null);
    }

    // Mock forecast는 잔여 전체 월급 합계만 제공하므로, 실제 월별 forecast API가 생기기 전까지 월 평균으로 배분한다.
    long monthlySalary = input.expectedSalary() / remainingMonths;
    long monthlyInvestment = percentageOf(monthlySalary, request.getInvestmentRatio());
    // 소비액은 월 저축·투자 원금으로 늘어나는 자산에서 차감한다.
    // 위키 원문의 누적식에는 소비 차감이 누락되어 있었지만, 이를 빼지 않으면 소비 슬라이더가 결과에 영향을 주지 않는다.
    long monthlyNetAssetChange =
        input.mandatorySavingAmount()
            + request.getMonthlySavingAmount()
            + monthlyInvestment
            - request.getMonthlySpendingAmount();
    long investmentReturn =
        percentageOf(monthlyInvestment * remainingMonths, request.getExpectedReturnRate());
    long savingInterest =
        percentageOf(input.mandatorySavingAmount() * remainingMonths, input.savingInterestRate());

    LocalDate financialDischargeDate = null;
    YearMonth month = YearMonth.from(calculationDate).plusMonths(1);
    for (int index = 0; index < remainingMonths; index++) {
      asset += monthlyNetAssetChange;
      if (index == remainingMonths - 1) {
        asset += input.governmentSupportExpected() + savingInterest + investmentReturn;
      }
      if (financialDischargeDate == null && asset >= input.targetAmount()) {
        financialDischargeDate = month.atDay(1);
      }
      month = month.plusMonths(1);
    }

    return new SimulationCalculationResult(asset, financialDischargeDate);
  }

  private int remainingMonths(LocalDate calculationDate, LocalDate dischargeDate) {
    YearMonth firstForecastMonth = YearMonth.from(calculationDate).plusMonths(1);
    YearMonth dischargeMonth = YearMonth.from(dischargeDate);
    if (firstForecastMonth.isAfter(dischargeMonth)) {
      return 0;
    }
    return (int) (ChronoUnit.MONTHS.between(firstForecastMonth, dischargeMonth) + 1);
  }

  private long percentageOf(long amount, BigDecimal percentage) {
    return BigDecimal.valueOf(amount)
        .multiply(percentage)
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
