package com.jaedaero.domain.marketreport.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.dto.MarketCondition;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.service.MilitaryProductSummaryFactory;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketReportServiceImplTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(
          Instant.parse("2026-08-06T10:00:00Z"),
          ZoneId.of("Asia/Seoul"));

  @Test
  void getTodayReturnsActiveStructuredReport() {
    DailyMarketReportVo active = report(1L, "정상 리포트");
    var service = service(active, null);

    var response = service.getToday();

    assertEquals(MarketReportStatus.NORMAL, response.getReportStatus());
    assertEquals("정상 리포트", response.getContent());
    assertEquals(1, response.getIndicators().size());
    assertEquals(MarketIndicatorType.KOSPI, response.getIndicators().get(0).getIndicatorType());
    assertEquals(new BigDecimal("5.00"), response.getMilitaryProductSummary().getAnnualInterestRate());
    assertEquals("큰 변동이 없는 시기이니 기존 계획을 꾸준히 유지해보세요.", response.getRecommendedAction());
  }

  @Test
  void getTodayMarksLatestReportStaleWhenNoActiveReportExists() {
    var response = service(null, report(2L, "어제 리포트")).getToday();

    assertEquals(MarketReportStatus.STALE, response.getReportStatus());
    assertEquals("어제 리포트", response.getContent());
  }

  @Test
  void getTodayThrowsWhenNoReportExists() {
    assertThrows(MarketReportException.class, () -> service(null, null).getToday());
  }

  private MarketReportServiceImpl service(
      DailyMarketReportVo active, DailyMarketReportVo latest) {
    return new MarketReportServiceImpl(
        new StubReportMapper(active, latest),
        new StubIndicatorMapper(),
        new MilitaryProductSummaryFactory(),
        FIXED_CLOCK);
  }

  private DailyMarketReportVo report(long id, String content) {
    return DailyMarketReportVo.builder()
        .reportId(id)
        .reportStatus(MarketReportStatus.NORMAL)
        .marketCondition(MarketCondition.NEUTRAL)
        .content(content)
        .build();
  }

  private static class StubReportMapper implements DailyMarketReportMapper {
    private final DailyMarketReportVo active;
    private final DailyMarketReportVo latest;

    StubReportMapper(DailyMarketReportVo active, DailyMarketReportVo latest) {
      this.active = active;
      this.latest = latest;
    }

    @Override
    public int upsert(DailyMarketReportVo report) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DailyMarketReportVo findActiveAt(LocalDateTime now) {
      return active;
    }

    @Override
    public DailyMarketReportVo findLatest() {
      return latest;
    }

    @Override
    public int countByReportDate(java.time.LocalDate reportDate) {
      throw new UnsupportedOperationException();
    }
  }

  private static class StubIndicatorMapper
      implements DailyMarketIndicatorMapper {
    @Override
    public int insert(DailyMarketIndicatorVo indicator) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DailyMarketIndicatorVo> findByReportId(long reportId) {
      return List.of(
          DailyMarketIndicatorVo.builder()
              .indicatorType(MarketIndicatorType.KOSPI)
              .observedValue(BigDecimal.TEN)
              .status(MarketIndicatorStatus.NORMAL)
              .build());
    }
  }
}
