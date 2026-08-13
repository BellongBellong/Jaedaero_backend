package com.jaedaero.domain.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DischargeReportServiceImplTest {

  @Test
  void combinesDashboardAndLatestCashflowForTheSameUser() {
    RecordingDashboardService dashboardService = new RecordingDashboardService();
    RecordingCashflowService cashflowService = new RecordingCashflowService();
    DischargeReportService service = new DischargeReportServiceImpl(dashboardService, cashflowService);

    var response = service.get(7L);

    assertEquals(7L, dashboardService.userId);
    assertEquals(7L, cashflowService.userId);
    assertEquals(900L, response.getExpectedAssetGrowth());
    assertEquals(LocalDate.of(2027, 3, 15), response.getFinancialDischargeDate());
  }

  private static class RecordingDashboardService implements DashboardService {
    private long userId;

    @Override
    public DashboardResponse get(long userId) {
      this.userId = userId;
      return DashboardResponse.builder().financialDischargeDate(LocalDate.of(2027, 3, 15)).build();
    }
  }

  private static class RecordingCashflowService implements CashflowService {

    @Override
    public com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse getCalculationInput(
        long userId) {
      throw new UnsupportedOperationException();
    }

    private long userId;

    @Override public CashflowForecastResponse generate(long userId) { throw new UnsupportedOperationException(); }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      this.userId = userId;
      return CashflowForecastResponse.builder()
          .forecastId(1L).baseAsset(100L).expectedAsset(1_000L)
          .achievementRate(BigDecimal.TEN).build();
    }

    @Override public CashflowForecastResponse getLatest(long userId, int months) { throw new UnsupportedOperationException(); }
  }
}
