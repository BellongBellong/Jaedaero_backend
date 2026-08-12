package com.jaedaero.domain.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
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
    StrategyApplicationMapper strategyApplicationMapper = new EmptyStrategyApplicationMapper();
    DashboardServiceImpl service =
        new DashboardServiceImpl(
            cashflowService,
            dashboardMapper,
            strategyApplicationMapper,
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    DashboardResponse response = service.get(1L);

    assertEquals(1, cashflowService.generateCount);
    assertEquals(1_000_000L, response.getCurrentAsset());
    assertEquals(10_000_000L, response.getExpectedAsset());
  }

  private static class EmptyStrategyApplicationMapper implements StrategyApplicationMapper {
    @Override
    public int insert(StrategyApplicationVo strategyApplication) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int updateAfterExpectedAsset(long applicationId, long userId, long afterExpectedAsset) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationVo findByIdAndUserId(long applicationId, long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationVo findByAnalysisIdAndUserId(long analysisId, long userId) {
      return null;
    }

    @Override
    public StrategyApplicationVo findLatestByUserId(long userId) {
      return null;
    }

    @Override
    public StrategyApplicationVo findByGuidanceSelection(
        long userId,
        long guidanceId,
        InvestmentGuidanceAction action,
        InvestmentFrequency frequency,
        long contributionAmount) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationVo findLatestByGuidanceIdAndUserId(long guidanceId, long userId) {
      return null;
    }

    @Override
    public List<StrategyApplicationVo> findByUserId(long userId, long offset, int limit) {
      throw new UnsupportedOperationException();
    }
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
