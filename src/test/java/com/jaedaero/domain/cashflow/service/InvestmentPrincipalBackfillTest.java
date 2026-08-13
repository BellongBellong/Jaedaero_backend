package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvestmentPrincipalBackfillTest {

  @Test
  void sumsPastMonthlySalaryTimesRatioFromEnlistmentUpToCalculationMonth() {
    MilitaryPayPolicy payPolicy =
        (soldierType, enlistmentMonth, month) -> new DefaultMilitaryPayPolicy.MilitaryPay("이병", 400_000L);
    InvestmentPrincipalBackfill backfill = new InvestmentPrincipalBackfill(payPolicy);

    long estimate =
        backfill.estimate(
            SoldierType.ARMY,
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 4, 15),
            new BigDecimal("0.1000"));

    // 3 past months (Jan, Feb, Mar) * 400,000 * 10% = 120,000
    assertEquals(120_000L, estimate);
  }

  @Test
  void zeroRatioProducesZeroWithoutCallingThePayPolicy() {
    InvestmentPrincipalBackfill backfill =
        new InvestmentPrincipalBackfill(
            (soldierType, enlistmentMonth, month) -> {
              throw new AssertionError("should not resolve pay when ratio is zero");
            });

    long estimate =
        backfill.estimate(SoldierType.ARMY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), BigDecimal.ZERO);

    assertEquals(0L, estimate);
  }
}
