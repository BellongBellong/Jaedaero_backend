package com.jaedaero.domain.investmentguidance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.ServiceStage;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvestmentGuidanceCalculatorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);
  private static final LocalDate DISCHARGE_DATE = LocalDate.of(2027, 8, 4);
  private final InvestmentGuidanceCalculator calculator = new InvestmentGuidanceCalculator();

  @Test
  void safeFocusWhenZeroContributionStillCoversGoalAndOneMonthBuffer() {
    InvestmentGuidanceCalculationResult result =
        calculate(20_300_000L, 20_000_000L, 150_000L, 200_000L, "병장");

    assertEquals(InvestmentGuidanceAction.SAFE_FOCUS, result.actionType());
    assertEquals(ServiceStage.SERGEANT_PREPARE, result.serviceStage());
    assertEquals(0L, result.recommendedContributionAmount());
    assertEquals(20_300_000L, result.recommendedExpectedAsset());
  }

  @Test
  void pauseWhenGoalIsCoveredButOneMonthBufferIsNot() {
    InvestmentGuidanceCalculationResult result =
        calculate(20_100_000L, 20_000_000L, 150_000L, 200_000L, "상병");

    assertEquals(InvestmentGuidanceAction.PAUSE, result.actionType());
    assertEquals(ServiceStage.CORPORAL_CHECK, result.serviceStage());
    assertEquals(0L, result.recommendedContributionAmount());
  }

  @Test
  void reduceToMinimumThousandWonUnitWhenSmallerContributionCoversGoal() {
    InvestmentGuidanceCalculationResult result =
        calculate(19_940_000L, 20_000_000L, 200_000L, 250_000L, "일병");

    assertEquals(InvestmentGuidanceAction.REDUCE, result.actionType());
    assertEquals(ServiceStage.PRIVATE_FIRST_CLASS_GROWTH, result.serviceStage());
    assertTrue(result.recommendedContributionAmount() > 0);
    assertTrue(result.recommendedContributionAmount() < 200_000L);
    assertEquals(0L, result.recommendedContributionAmount() % 1_000L);
    assertTrue(result.recommendedExpectedAsset() >= 20_000_000L);
  }

  @Test
  void continueWhenEvenCurrentContributionDoesNotReachGoal() {
    InvestmentGuidanceCalculationResult result =
        calculate(19_000_000L, 20_000_000L, 150_000L, 200_000L, "이병");

    assertEquals(InvestmentGuidanceAction.CONTINUE, result.actionType());
    assertEquals(150_000L, result.recommendedContributionAmount());
  }

  @Test
  void startWithAnAmountWithinMonthlyLimitWhenPlanIsPausedAtZero() {
    InvestmentGuidanceCalculationResult result =
        calculate(19_950_000L, 20_000_000L, 0L, 200_000L, "훈련병");

    assertEquals(InvestmentGuidanceAction.START, result.actionType());
    assertTrue(result.recommendedContributionAmount() > 0);
    assertTrue(result.recommendedContributionAmount() <= 200_000L);
  }

  private InvestmentGuidanceCalculationResult calculate(
      long baseline,
      long target,
      long contribution,
      long maximumMonthlyAmount,
      String rankName) {
    RecurringInvestmentPlanVo plan =
        RecurringInvestmentPlanVo.builder()
            .frequency(InvestmentFrequency.MONTHLY)
            .contributionDay(10)
            .contributionAmount(contribution)
            .maximumMonthlyAmount(maximumMonthlyAmount)
            .build();
    return calculator.calculate(
        new InvestmentGuidanceCalculationInput(
            baseline,
            target,
            rankName,
            DISCHARGE_DATE,
            new BigDecimal("10.00"),
            plan),
        TODAY);
  }
}
