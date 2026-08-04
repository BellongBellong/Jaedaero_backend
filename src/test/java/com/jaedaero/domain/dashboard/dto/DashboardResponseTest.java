package com.jaedaero.domain.dashboard.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.cashflow.dto.CashflowForecastMonthResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardResponseTest {

  @Test
  void mapsLatestCashflowAndCurrentMonthValues() {
    LocalDate today = LocalDate.of(2026, 8, 4);
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder()
            .baseAsset(3_000_000L)
            .expectedAsset(15_000_000L)
            .achievementRate(new BigDecimal("75.00"))
            .financialDischargeDate(LocalDate.of(2027, 8, 20))
            .months(
                List.of(
                    CashflowForecastMonthResponse.builder()
                        .forecastMonth(LocalDate.of(2026, 8, 1))
                        .expectedSavingAmount(550_000L)
                        .expectedSpendingAmount(0L)
                        .build()))
            .build();

    DashboardResponse response =
        DashboardResponse.from(cashflow, LocalDate.of(2027, 9, 1), today);

    assertEquals(12L, response.getDeltaDaysVsActual());
    assertEquals(3_000_000L, response.getCurrentAsset());
    assertEquals(15_000_000L, response.getExpectedAsset());
    assertEquals(0L, response.getThisMonthSpending());
    assertEquals(550_000L, response.getThisMonthSaving());
  }
}
