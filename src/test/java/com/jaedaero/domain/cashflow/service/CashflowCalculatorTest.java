package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CashflowCalculatorTest {

  private final CashflowCalculator calculator = new CashflowCalculator(new DefaultMilitaryPayPolicy());

  @Test
  void uses2026BasicPayAndRankPromotionSchedule() {
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
    assertEquals("PRIVATE", result.months().get(0).expectedRank());
    assertEquals(750_000L, result.months().get(0).expectedSalary());
    assertEquals("PRIVATE_FIRST_CLASS", result.months().get(2).expectedRank());
    assertEquals("CORPORAL", result.months().get(8).expectedRank());
    assertEquals("SERGEANT", result.months().get(14).expectedRank());
    assertEquals(20_100_000L, result.expectedSalary());
  }

  @Test
  void targetAlreadyReached_marksTodayAsFinancialDischargeDate() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(3_000_000L, 1_000_000L, 0L, LocalDate.of(2026, 2, 1)),
            LocalDate.of(2026, 1, 10));

    assertEquals(LocalDate.of(2026, 1, 10), result.financialDischargeDate());
    assertEquals(0L, result.months().get(0).expectedSavingAmount());
    assertEquals(750_000L, result.monthlySpendingLimit());
  }

  @Test
  void zeroTarget_hasFullAchievementRateWithoutDivisionByZero() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 0L, 0L, LocalDate.of(2026, 1, 1)), LocalDate.of(2026, 1, 10));

    assertEquals(100D, result.achievementRate());
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
  void spendingAboveSalary_keepsSpendingLimitAtZeroAndReducesAsset() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 10_000_000L, 1_000_000L, LocalDate.of(2026, 1, 1)),
            LocalDate.of(2026, 1, 10));

    assertEquals(0L, result.monthlySpendingLimit());
    assertEquals(-250_000L, result.months().get(0).expectedEndingAsset());
  }

  @Test
  void recalculatesRequiredSavingAndFindsFinancialDischargeMonth() {
    CashflowForecastCalculation result =
        calculator.calculate(
            input(0L, 1_400_000L, 0L, LocalDate.of(2026, 2, 1)), LocalDate.of(2026, 1, 10));

    assertEquals(700_000L, result.months().get(0).expectedSavingAmount());
    assertEquals(650_000L, result.months().get(1).expectedSavingAmount());
    assertEquals(LocalDate.of(2026, 2, 1), result.financialDischargeDate());
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
}
