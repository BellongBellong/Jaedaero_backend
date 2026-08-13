package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.mapper.MilitaryPayPolicyMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CashflowCalculatorTest {

  private final CashflowCalculator calculator = new CashflowCalculator(policy());

  @Test
  void usesDatabasePayAndRankPromotionSchedule() {
    CashflowInput input =
        new CashflowInput(
            0L,
            30_000_000L,
            0L,
            SoldierType.ARMY,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 6, 1));

    CashflowForecastCalculation result = calculator.calculate(input, LocalDate.of(2026, 1, 10));

    assertEquals(18, result.months().size());
    assertEquals("이병", result.months().get(0).expectedRank());
    assertEquals(200_000L, result.months().get(0).expectedSalary());
    assertEquals("일병", result.months().get(2).expectedRank());
    assertEquals("상병", result.months().get(8).expectedRank());
    assertEquals("병장", result.months().get(14).expectedRank());
    assertEquals(10_200_000L, result.expectedSalary());
    assertEquals(
        2_200_000L,
        result.months().stream().mapToLong(CashflowForecastMonthCalculation::expectedInvestmentAmount).sum());
  }

  @Test
  void separatesNavyAndAirForcePromotionSchedules() {
    assertEquals(
        "일병",
        calculator
            .calculate(
                new CashflowInput(
                    0L,
                    0L,
                    0L,
                    SoldierType.NAVY,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 1)),
                LocalDate.of(2026, 3, 1))
            .months()
            .get(0)
            .expectedRank());
    assertEquals(
        "이병",
        calculator
            .calculate(
                new CashflowInput(
                    0L,
                    0L,
                    0L,
                    SoldierType.AIRFORCE,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 1)),
                LocalDate.of(2026, 3, 1))
            .months()
            .get(0)
            .expectedRank());
  }

  @Test
  void excludesUnrealizedSavingBenefitsFromConservativeExpectedAsset() {
    CashflowForecastCalculation result =
        calculator.calculate(
            new CashflowInput(
                0L,
                10_000_000L,
                0L,
                SoldierType.ARMY,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                List.of(
                    new SoldierSavingInput(
                        1_000_000L,
                        500_000L,
                        BigDecimal.valueOf(12),
                        300_000L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 3, 31)))),
            LocalDate.of(2026, 1, 1));

    assertEquals(200_000L, result.months().get(0).expectedSavingAmount());
    assertEquals(750_000L, result.expectedSavingAmount());
    assertEquals(750_000L, result.expectedAsset());
  }

  @Test
  void targetAlreadyReached_marksTodayAsFinancialDischargeDate() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(3_000_000L, 1_000_000L, 0L, LocalDate.of(2026, 2, 1)),
            LocalDate.of(2026, 1, 10));

    assertEquals(LocalDate.of(2026, 1, 10), result.financialDischargeDate());
    assertEquals(200_000L, result.months().get(0).expectedSavingAmount());
    assertEquals(200_000L, result.monthlySpendingLimit());
  }

  @Test
  void estimatesFinancialDischargeDayWithinTheTargetMonth() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 100_000L, 0L, LocalDate.of(2026, 1, 31)),
            LocalDate.of(2026, 1, 10));

    assertEquals(LocalDate.of(2026, 1, 20), result.financialDischargeDate());
  }

  @Test
  void zeroTarget_hasFullAchievementRateWithoutDivisionByZero() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 0L, 0L, LocalDate.of(2026, 1, 31)), LocalDate.of(2026, 1, 10));

    assertEquals(100D, result.achievementRate());
    assertEquals(200_000L, result.months().get(0).expectedSavingAmount());
    assertEquals(0L, result.months().get(0).expectedSpendingAmount());
  }

  @Test
  void pastDischargeDate_doesNotCreateForecastMonths() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(1_000_000L, 2_000_000L, 0L, LocalDate.of(2025, 12, 1)),
            LocalDate.of(2026, 1, 10));

    assertEquals(0, result.months().size());
    assertEquals(1_000_000L, result.expectedAsset());
    assertNull(result.financialDischargeDate());
  }

  @Test
  void pastDischargeDayInTheSameMonth_doesNotCreateForecastMonths() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(1_000_000L, 2_000_000L, 0L, LocalDate.of(2026, 1, 1)),
            LocalDate.of(2026, 1, 10));

    assertEquals(0, result.months().size());
    assertEquals(1_000_000L, result.expectedAsset());
    assertNull(result.financialDischargeDate());
  }

  @Test
  void usesRecentSpendingAverageWhenNoPlanHasBeenApplied() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 10_000_000L, 1_000_000L, LocalDate.of(2026, 1, 31)),
            LocalDate.of(2026, 1, 10));

    assertEquals(0L, result.monthlySpendingLimit());
    assertEquals(-800_000L, result.months().get(0).expectedEndingAsset());
  }

  @Test
  void recalculatesRequiredSavingAndFindsFinancialDischargeMonth() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 1_400_000L, 0L, LocalDate.of(2026, 2, 1)), LocalDate.of(2026, 1, 10));

    assertEquals(200_000L, result.months().get(0).expectedSavingAmount());
    assertEquals(200_000L, result.months().get(1).expectedSavingAmount());
    assertNull(result.financialDischargeDate());
  }

  @Test
  void appliedAiStrategyOverridesSpendingAndSavingForEveryForecastMonth() {
    AppliedCashflowStrategy strategy =
        new AppliedCashflowStrategy(
            9L, 50_000L, 100_000L, 50_000L, new BigDecimal("5.00"));
    CashflowInput input =
        new CashflowInput(
            0L,
            10_000_000L,
            999_999L,
            SoldierType.ARMY,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            List.of(),
            strategy);

    CashflowForecastCalculation result =
        calculator.calculate(input, LocalDate.of(2026, 1, 10));

    assertEquals(0L, result.monthlySpendingLimit());
    assertEquals(200_000L, result.expectedSavingAmount());
    assertEquals(300_000L, result.expectedAsset());
    assertEquals(50_000L, result.months().get(0).expectedSpendingAmount());
    assertEquals(100_000L, result.months().get(0).expectedSavingAmount());
    assertEquals(50_000L, result.months().get(0).expectedInvestmentAmount());
    assertEquals(50_000L, result.months().get(1).expectedSpendingAmount());
    assertEquals(100_000L, result.months().get(1).expectedSavingAmount());
    assertEquals(50_000L, result.months().get(1).expectedInvestmentAmount());
  }

  @Test
  void appliedSavingGoalReportsOnlyMonthlyContributions() {
    AppliedCashflowStrategy strategy =
        new AppliedCashflowStrategy(9L, 0L, 100_000L, 0L, new BigDecimal("5.00"));
    CashflowInput input =
        new CashflowInput(
            1_000_000L,
            10_000_000L,
            0L,
            SoldierType.ARMY,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            List.of(
                new SoldierSavingInput(
                    1_000_000L,
                    500_000L,
                    BigDecimal.valueOf(12),
                    0L,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31))),
            strategy);

    CashflowForecastCalculation result = calculator.calculate(input, LocalDate.of(2026, 1, 1));

    assertEquals(300_000L, result.expectedSavingAmount());
  }

  @Test
  void appliedAiStrategyOverMonthlySalaryIsRejected() {
    AppliedCashflowStrategy strategy =
        new AppliedCashflowStrategy(
            9L, 100_000L, 100_000L, 1L, new BigDecimal("5.00"));
    CashflowInput input =
        new CashflowInput(
            0L,
            10_000_000L,
            0L,
            SoldierType.ARMY,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            List.of(),
            strategy);

    assertThrows(
        CashflowException.class,
        () -> calculator.calculate(input, LocalDate.of(2026, 1, 10)));
  }

  private CashflowInput input(
      long baseAsset, long targetAmount, long monthlySpending, LocalDate dischargeDate) {
    return new CashflowInput(
        baseAsset,
        targetAmount,
        monthlySpending,
        SoldierType.ARMY,
        LocalDate.of(2026, 1, 1),
        dischargeDate);
  }

  private DefaultMilitaryPayPolicy policy() {
    Map<String, Long> salaries =
        Map.of("이병", 200_000L, "일병", 350_000L, "상병", 650_000L, "병장", 950_000L);
    MilitaryPayPolicyMapper mapper =
        (soldierType, rankName, monthStart, monthEnd) -> salaries.get(rankName);
    return new DefaultMilitaryPayPolicy(mapper);
  }
}
