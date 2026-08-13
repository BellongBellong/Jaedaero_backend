package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketReportIndicatorRefreshServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void refreshesOnlyIndicatorsAndPromotesPartialGeminiReportWhenAllAreNormal() {
    RecordingPersistence persistence = new RecordingPersistence();
    MarketReportIndicatorRefreshService service =
        service(report(MarketReportStatus.PARTIAL), persistence);

    service.refreshToday();

    assertEquals(LocalDate.of(2026, 8, 12), persistence.reportDate);
    assertEquals(MarketReportStatus.NORMAL, persistence.reportStatus);
    assertEquals(4, persistence.indicators.size());
  }

  @Test
  void rejectsRefreshForAlreadyNormalReport() {
    MarketReportIndicatorRefreshService service =
        service(report(MarketReportStatus.NORMAL), new RecordingPersistence());

    MarketReportException exception =
        assertThrows(MarketReportException.class, service::refreshToday);

    assertEquals(MarketReportErrorCode.INDICATOR_REFRESH_NOT_AVAILABLE, exception.getErrorCode());
  }

  private MarketReportIndicatorRefreshService service(
      DailyMarketReportVo report, RecordingPersistence persistence) {
    MarketIndicatorProvider provider = ignored -> normalIndicators();
    return new MarketReportIndicatorRefreshService(
        new StubReportMapper(report), provider, persistence, CLOCK);
  }

  private List<MarketIndicatorResult> normalIndicators() {
    return Arrays.stream(MarketIndicatorType.values())
        .map(
            type ->
                MarketIndicatorResult.of(
                    new MarketIndicatorObservation(
                        type,
                        LocalDate.of(2026, 8, 12),
                        "test",
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO),
                    MarketIndicatorStatus.NORMAL))
        .toList();
  }

  private DailyMarketReportVo report(MarketReportStatus status) {
    return DailyMarketReportVo.builder()
        .reportId(1L)
        .reportDate(LocalDate.of(2026, 8, 12))
        .reportStatus(status)
        .generationSource(MarketReportGenerationSource.GEMINI)
        .build();
  }

  private static class RecordingPersistence extends MarketReportPersistenceService {
    private LocalDate reportDate;
    private List<MarketIndicatorResult> indicators;
    private MarketReportStatus reportStatus;

    RecordingPersistence() {
      super(null, null, null);
    }

    @Override
    public void refreshIndicators(
        LocalDate reportDate,
        List<MarketIndicatorResult> indicators,
        MarketReportStatus reportStatus) {
      this.reportDate = reportDate;
      this.indicators = indicators;
      this.reportStatus = reportStatus;
    }
  }

  private static class StubReportMapper implements DailyMarketReportMapper {
    private final DailyMarketReportVo report;

    StubReportMapper(DailyMarketReportVo report) {
      this.report = report;
    }

    @Override public int upsert(DailyMarketReportVo ignored) { throw new UnsupportedOperationException(); }
    @Override public DailyMarketReportVo findActiveAt(LocalDateTime now) { throw new UnsupportedOperationException(); }
    @Override public DailyMarketReportVo findLatest() { throw new UnsupportedOperationException(); }
    @Override public int countByReportDate(LocalDate reportDate) { throw new UnsupportedOperationException(); }
    @Override public int claimReportDate(LocalDate reportDate, LocalDateTime validFrom, LocalDateTime validUntil) { throw new UnsupportedOperationException(); }
    @Override public DailyMarketReportVo findByReportDateForUpdate(LocalDate reportDate) { throw new UnsupportedOperationException(); }
    @Override public DailyMarketReportVo findByReportDate(LocalDate reportDate) { return report; }
    @Override public int markGenerationInProgress(long reportId) { throw new UnsupportedOperationException(); }
    @Override public int updateReportStatus(long reportId, String reportStatus) { throw new UnsupportedOperationException(); }
  }
}
