package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.ServiceStage;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentSchedule;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InvestmentGuidanceCalculator {

  private static final long RECOMMENDATION_UNIT = 1_000L;

  public InvestmentGuidanceCalculationResult calculate(
      InvestmentGuidanceCalculationInput input, LocalDate calculationDate) {
    RecurringInvestmentPlanVo plan = input.plan();
    List<LocalDate> contributionDates =
        RecurringInvestmentSchedule.datesUntil(
            calculationDate,
            input.dischargeDate(),
            plan.getFrequency(),
            plan.getContributionDay());
    long currentAmount = plan.getContributionAmount();
    long safetyBuffer = plan.getMaximumMonthlyAmount();
    long continueExpected = expectedAsset(input, currentAmount, contributionDates);
    long zeroExpected = expectedAsset(input, 0, contributionDates);

    InvestmentGuidanceAction action;
    long recommendedAmount;
    if (contributionDates.isEmpty()) {
      action = InvestmentGuidanceAction.REVIEW;
      recommendedAmount = currentAmount;
    } else if (zeroExpected >= safeAdd(input.targetAmount(), safetyBuffer)) {
      action = InvestmentGuidanceAction.SAFE_FOCUS;
      recommendedAmount = 0;
    } else if (zeroExpected >= input.targetAmount()) {
      action = InvestmentGuidanceAction.PAUSE;
      recommendedAmount = 0;
    } else if (currentAmount == 0) {
      long maximum =
          RecurringInvestmentSchedule.maximumContributionPerCycle(
              plan.getFrequency(), plan.getMaximumMonthlyAmount());
      if (maximum == 0) {
        action = InvestmentGuidanceAction.REVIEW;
        recommendedAmount = 0;
      } else {
        action = InvestmentGuidanceAction.START;
        recommendedAmount = minimumRequired(input, contributionDates, maximum);
        if (recommendedAmount == 0) recommendedAmount = maximum;
      }
    } else if (continueExpected < input.targetAmount()) {
      action = InvestmentGuidanceAction.CONTINUE;
      recommendedAmount = currentAmount;
    } else {
      recommendedAmount = minimumRequired(input, contributionDates, currentAmount);
      action =
          recommendedAmount < currentAmount
              ? InvestmentGuidanceAction.REDUCE
              : InvestmentGuidanceAction.CONTINUE;
    }

    return new InvestmentGuidanceCalculationResult(
        ServiceStage.fromRankName(input.rankName()),
        action,
        currentAmount,
        recommendedAmount,
        continueExpected,
        expectedAsset(input, recommendedAmount, contributionDates),
        contributionDates.size(),
        safetyBuffer);
  }

  private long minimumRequired(
      InvestmentGuidanceCalculationInput input,
      List<LocalDate> contributionDates,
      long maximumAmount) {
    if (maximumAmount <= 0
        || expectedAsset(input, maximumAmount, contributionDates) < input.targetAmount()) {
      return maximumAmount;
    }
    long low = 0;
    long high = maximumAmount;
    while (low < high) {
      long middle = low + (high - low) / 2;
      if (expectedAsset(input, middle, contributionDates) >= input.targetAmount()) {
        high = middle;
      } else {
        low = middle + 1;
      }
    }
    long rounded = ((low + RECOMMENDATION_UNIT - 1) / RECOMMENDATION_UNIT) * RECOMMENDATION_UNIT;
    return Math.min(maximumAmount, rounded);
  }

  private long expectedAsset(
      InvestmentGuidanceCalculationInput input,
      long contributionAmount,
      List<LocalDate> contributionDates) {
    BigDecimal annualRate =
        input.expectedReturnRate() == null
            ? BigDecimal.ZERO
            : input.expectedReturnRate().max(BigDecimal.ZERO);
    BigDecimal dayWeights =
        contributionDates.stream()
            .map(date -> BigDecimal.valueOf(ChronoUnit.DAYS.between(date, input.dischargeDate())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long expectedReturnPremium =
        BigDecimal.valueOf(contributionAmount)
            .multiply(annualRate)
            .multiply(dayWeights)
            .divide(BigDecimal.valueOf(36_500), 0, RoundingMode.HALF_UP)
            .longValueExact();
    return Math.addExact(input.baselineExpectedAsset(), expectedReturnPremium);
  }

  private long safeAdd(long left, long right) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException exception) {
      return Long.MAX_VALUE;
    }
  }
}
