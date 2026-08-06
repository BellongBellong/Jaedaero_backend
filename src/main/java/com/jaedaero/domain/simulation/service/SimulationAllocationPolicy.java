package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** What-if 월 배분금액의 제약과 표시용 비율 계산을 담당한다. */
@Component
public class SimulationAllocationPolicy {

  public static final long MAX_MONTHLY_SAVING_AMOUNT = 550_000L;
  public static final BigDecimal DEFAULT_EXPECTED_RETURN_RATE = new BigDecimal("5.00");

  public void validate(SimulationRequest request, long referenceMonthlyIncome) {
    if (referenceMonthlyIncome <= 0) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY, "계산 기준월의 군 월급 정보가 없습니다.");
    }

    long spending = requiredNonNegative(request.getMonthlySpendingAmount(), "월 소비액");
    long saving =
        requiredNonNegative(request.getMonthlySavingAmount(), "장병내일준비적금 월 납입액");
    long investment = requiredNonNegative(request.getMonthlyInvestmentAmount(), "월 투자금액");
    if (saving > MAX_MONTHLY_SAVING_AMOUNT) {
      throw invalid("장병내일준비적금 월 납입액은 550000원 이하여야 합니다.");
    }

    long allocated = allocatedAmount(spending, saving, investment);
    if (allocated > referenceMonthlyIncome) {
      throw invalid("월 소비·군적금·투자 금액의 합은 계산 기준월 군 월급을 초과할 수 없습니다.");
    }
  }

  public SimulationAllocationMetrics metrics(
      long monthlySpendingAmount,
      long monthlySavingAmount,
      long monthlyInvestmentAmount,
      long referenceMonthlyIncome) {
    long allocated =
        allocatedAmount(monthlySpendingAmount, monthlySavingAmount, monthlyInvestmentAmount);
    return new SimulationAllocationMetrics(
        referenceMonthlyIncome,
        rate(monthlySpendingAmount, referenceMonthlyIncome),
        rate(monthlySavingAmount, referenceMonthlyIncome),
        rate(monthlyInvestmentAmount, referenceMonthlyIncome),
        Math.max(0L, referenceMonthlyIncome - allocated));
  }

  private long requiredNonNegative(Long value, String fieldName) {
    if (value == null) {
      throw invalid(fieldName + "은 필수입니다.");
    }
    if (value < 0) {
      throw invalid(fieldName + "은 0 이상이어야 합니다.");
    }
    return value;
  }

  private long allocatedAmount(long spending, long saving, long investment) {
    try {
      return Math.addExact(Math.addExact(spending, saving), investment);
    } catch (ArithmeticException exception) {
      throw invalid("월 배분금액의 합이 허용 범위를 초과했습니다.");
    }
  }

  private BigDecimal rate(long amount, long referenceMonthlyIncome) {
    if (referenceMonthlyIncome <= 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return BigDecimal.valueOf(amount)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(referenceMonthlyIncome), 2, RoundingMode.HALF_UP);
  }

  private SimulationException invalid(String message) {
    return new SimulationException(SimulationErrorCode.INVALID_REQUEST, message);
  }
}
