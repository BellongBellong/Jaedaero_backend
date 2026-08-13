package com.jaedaero.domain.report.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DischargeReportResponseTest {

  @Test
  void mapsCashflowIntoDischargeReport() {
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder()
            .forecastId(12L)
            .baseAsset(1_000_000L)
            .expectedSalary(14_500_000L)
            .expectedSavingAmount(18_000_000L)
            .expectedAsset(21_500_000L)
            .achievementRate(new BigDecimal("107.50"))
            .build();
    DashboardResponse dashboard =
        DashboardResponse.builder()
            .financialDischargeDate(LocalDate.of(2027, 3, 15))
            .actualDischargeDate(LocalDate.of(2027, 6, 20))
            .deltaDaysVsActual(97L)
            .build();

    DischargeReportResponse response = DischargeReportResponse.from(cashflow, dashboard);

    assertEquals(20_500_000L, response.getExpectedAssetGrowth());
    assertEquals(new BigDecimal("2050.00"), response.getExpectedAssetGrowthRate());
    assertEquals(LocalDate.of(2027, 3, 15), response.getFinancialDischargeDate());
  }

  @Test
  void omitsGrowthRateWhenBaseAssetIsZero() {
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder().baseAsset(0L).expectedAsset(1_000_000L).build();
    DashboardResponse dashboard = DashboardResponse.builder().build();

    assertNull(DischargeReportResponse.from(cashflow, dashboard).getExpectedAssetGrowthRate());
  }
}
