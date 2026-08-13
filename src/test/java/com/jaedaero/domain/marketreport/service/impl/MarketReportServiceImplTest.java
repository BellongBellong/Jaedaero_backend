package com.jaedaero.domain.marketreport.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportSourceVo;
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
      Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void getTodayReturnsActiveStructuredMarketReportWithFourIndicatorsAndSources() {
    DailyMarketReportVo active = report(1L, "정상 시장 리포트");
    var service = service(active, null);

    var response = service.getToday();

    assertEquals(MarketReportStatus.NORMAL, response.getReportStatus());
    assertEquals("오늘의 테스트 시장 리포트", response.getTitle());
    assertEquals("확인된 사실을 한 줄로 정리한 테스트 요약", response.getSummary());
    assertEquals("정상 시장 리포트", response.getContent());
    assertEquals(4, response.getIndicators().size());
    assertEquals(MarketIndicatorType.KOSPI, response.getIndicators().get(0).getIndicatorType());
    assertEquals(MarketIndicatorType.KOSDAQ, response.getIndicators().get(1).getIndicatorType());
    assertEquals(MarketIndicatorType.US_TREASURY_10Y, response.getIndicators().get(2).getIndicatorType());
    assertEquals(MarketIndicatorType.USD_KRW, response.getIndicators().get(3).getIndicatorType());
    assertEquals(2, response.getSources().size());
    assertEquals("출처 제목", response.getSources().get(0).getTitle());
    assertEquals("https://example.com/source", response.getSources().get(0).getUrl());
    assertEquals("두 번째 출처", response.getSources().get(1).getTitle());
    assertEquals("https://example.org/source", response.getSources().get(1).getUrl());
    assertEquals(MarketReportGenerationSource.GEMINI, response.getGenerationSource());
  }

  @Test
  void getTodayFillsMissingIndicatorRowsAsMissingToKeepFourIndicatorContract() {
    var response = service(report(1L, "정상 시장 리포트"), null).getToday();

    assertEquals(MarketIndicatorStatus.MISSING, response.getIndicators().get(1).getStatus());
    assertEquals("N/A", response.getIndicators().get(1).getSource());
  }

  @Test
  void getTodayIndicatorsReturnsOnlyIndicatorContract() {
    var response = service(report(1L, "정상 시장 리포트"), null).getTodayIndicators();

    assertEquals(1L, response.getReportId());
    assertEquals(MarketReportStatus.NORMAL, response.getReportStatus());
    assertEquals(4, response.getIndicators().size());
  }

  @Test
  void getTodayMarksLatestReportStaleWhenNoActiveReportExists() {
    var response = service(null, report(2L, "어제 시장 리포트")).getToday();

    assertEquals(MarketReportStatus.STALE, response.getReportStatus());
    assertEquals("어제 시장 리포트", response.getContent());
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
        new StubSourceMapper(),
        FIXED_CLOCK);
  }

  private DailyMarketReportVo report(long id, String content) {
    return DailyMarketReportVo.builder()
        .reportId(id)
        .reportDate(java.time.LocalDate.of(2026, 8, 6))
        .title("오늘의 테스트 시장 리포트")
        .summary("확인된 사실을 한 줄로 정리한 테스트 요약")
        .reportStatus(MarketReportStatus.NORMAL)
        .generationSource(MarketReportGenerationSource.GEMINI)
        .modelName("gemini-3.6-flash")
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

    @Override
    public int claimReportDate(
        java.time.LocalDate reportDate, LocalDateTime validFrom, LocalDateTime validUntil) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DailyMarketReportVo findByReportDateForUpdate(java.time.LocalDate reportDate) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DailyMarketReportVo findByReportDate(java.time.LocalDate reportDate) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int markGenerationInProgress(long reportId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int updateReportStatus(long reportId, String reportStatus) {
      throw new UnsupportedOperationException();
    }
  }

  private static class StubIndicatorMapper implements DailyMarketIndicatorMapper {
    @Override
    public int insert(DailyMarketIndicatorVo indicator) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int deleteByReportId(long reportId) {
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

  private static class StubSourceMapper implements DailyMarketReportSourceMapper {
    @Override
    public int insert(DailyMarketReportSourceVo source) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int deleteByReportId(long reportId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DailyMarketReportSourceVo> findByReportId(long reportId) {
      return List.of(
          DailyMarketReportSourceVo.builder()
              .sourceOrder(1)
              .title("출처 제목")
              .url("https://example.com/source")
              .build(),
          DailyMarketReportSourceVo.builder()
              .sourceOrder(2)
              .title("두 번째 출처")
              .url("https://example.org/source")
              .build());
    }
  }
}
