package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ConservativeMonthlyCashflowEngineTest {

  private final ConservativeMonthlyCashflowEngine engine =
      new ConservativeMonthlyCashflowEngine();

  @Test
  void endingAssetIncludesOnlyOpeningAssetSalaryAndSpending() {
    ConservativeMonthlyCashflowEngine.MonthProjection result =
        engine.project(
            1_000_000L,
            600_000L,
            250_000L,
            10_000_000L,
            LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 12, 7),
            YearMonth.of(2026, 1));

    assertEquals(1_350_000L, result.endingAsset());
    assertNull(result.targetReachedDate());
  }

  @Test
  void targetDateIsCappedToTheActualDischargePeriod() {
    ConservativeMonthlyCashflowEngine.MonthProjection result =
        engine.project(
            0L,
            100_000L,
            0L,
            50_000L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 11),
            YearMonth.of(2026, 1));

    assertEquals(LocalDate.of(2026, 1, 6), result.targetReachedDate());
  }
}
