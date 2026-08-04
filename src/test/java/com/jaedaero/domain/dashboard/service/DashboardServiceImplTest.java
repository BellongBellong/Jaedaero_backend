package com.jaedaero.domain.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceImplTest {

  @Test
  void generatesInitialForecastWhenNoForecastExists() {
    CashflowForecastResponse generated =
        CashflowForecastResponse.builder()
            .baseAsset(1_000_000L)
            .expectedAsset(10_000_000L)
            .achievementRate(new BigDecimal("50.00"))
            .months(List.of())
            .build();
    RecordingCashflowService cashflowService = new RecordingCashflowService(generated);
    DashboardMapper dashboardMapper = userId -> LocalDate.of(2027, 6, 20);
    DashboardServiceImpl service =
        new DashboardServiceImpl(
            cashflowService,
            dashboardMapper,
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    DashboardResponse response = service.get(1L);

    assertEquals(1, cashflowService.generateCount);
    assertEquals(1_000_000L, response.getCurrentAsset());
    assertEquals(10_000_000L, response.getExpectedAsset());
  }

  private static class RecordingCashflowService implements CashflowService {
    private final CashflowForecastResponse generated;
    private int generateCount;

    private RecordingCashflowService(CashflowForecastResponse generated) {
      this.generated = generated;
    }

    @Override
    public CashflowForecastResponse generate(long userId) {
      generateCount++;
      return generated;
    }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      throw new CashflowException(CashflowErrorCode.NOT_FOUND, "예측이 없습니다.");
    }

    @Override
    public CashflowForecastResponse getLatest(long userId, int months) {
      return getLatest(userId);
    }
  }
}
