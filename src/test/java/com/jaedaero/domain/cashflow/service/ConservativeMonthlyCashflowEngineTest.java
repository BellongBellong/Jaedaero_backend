package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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
            250_000L);

    assertEquals(1_350_000L, result.endingAsset());
  }

  @Test
  void targetDateIsCappedToTheActualDischargePeriod() {
    LocalDate result =
        engine.estimateTargetReachedDate(
            0L,
            100_000L,
            50_000L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 11),
            YearMonth.of(2026, 1));

    assertEquals(LocalDate.of(2026, 1, 6), result);
  }

  @Test
  void existingSoldierSavingBalanceEarnsRoughSimpleInterestByElapsedMonths() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(
                new SoldierSavingInput(
                    12_000_000L, 0L, null, 0L,
                    LocalDate.of(2025, 1, 10),
                    LocalDate.of(2027, 1, 10))),
            LocalDate.of(2026, 1, 10),
            List.of(),
            List.of(),
            LocalDate.of(2027, 1, 10),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    // 평가 시점(2027-01-10)까지 24개월 누적 이자를 동일한 시간축으로 계산한다.
    assertEquals(1_200_000L, benefit.soldierSavingInterest());
    assertEquals(12_000_000L, benefit.soldierSavingPrincipal());
    assertEquals(12_000_000L, benefit.governmentMatchingSupport());
  }

  @Test
  void currentValuationIncludesAccruedSavingInterestAndGovernmentMatching() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(
                new SoldierSavingInput(
                    4_000_000L, 550_000L, null, 0L,
                    LocalDate.of(2026, 1, 14),
                    LocalDate.of(2026, 12, 7))),
            LocalDate.of(2026, 8, 14),
            List.of(),
            List.of(),
            LocalDate.of(2026, 8, 14),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    assertEquals(116_667L, benefit.soldierSavingInterest());
    assertEquals(4_000_000L, benefit.governmentMatchingSupport());
    assertEquals(8_116_667L, engine.unifiedAsset(4_000_000L, benefit));
  }

  @Test
  void futureSavingInstallmentsEarnInterestProratedByRemainingMonthsToMaturity() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(),
            LocalDate.of(2026, 1, 10),
            List.of(500_000L, 500_000L),
            List.of(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 7, 10)),
            LocalDate.of(2027, 1, 10),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    // 1st installment: 12 months left -> 500,000 * 5% * 12/12 = 25,000
    // 2nd installment: 6 months left  -> 500,000 * 5% * 6/12  = 12,500
    assertEquals(37_500L, benefit.soldierSavingInterest());
    assertEquals(1_000_000L, benefit.soldierSavingPrincipal());
    assertEquals(1_000_000L, benefit.governmentMatchingSupport());
  }

  @Test
  void savingMaturityFallsBackToDischargeDateWhenAccountHasNoEndDate() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(new SoldierSavingInput(0L, 0L, null, 0L, LocalDate.of(2026, 1, 10), null)),
            LocalDate.of(2026, 1, 10),
            List.of(1_000_000L),
            List.of(LocalDate.of(2026, 1, 10)),
            LocalDate.of(2026, 7, 10),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    // no end_date on the account -> falls back to dischargeDate (6 months out)
    // 1,000,000 * 5% * 6/12 = 25,000
    assertEquals(25_000L, benefit.soldierSavingInterest());
  }

  @Test
  void existingInvestmentPrincipalCompoundsAlongsideFutureContributions() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(),
            LocalDate.of(2026, 1, 10),
            List.of(),
            List.of(),
            LocalDate.of(2026, 1, 10),
            1_000_000L,
            List.of(0L, 0L, 0L),
            new java.math.BigDecimal("12.00"));

    // 1,000,000 compounding at 1%/month for 3 months: 1,000,000 * 1.01^3 - 1,000,000 = 30,301
    assertEquals(30_301L, benefit.expectedInvestmentReturn());
    assertEquals(1_000_000L, benefit.investmentPrincipal());
  }
}
