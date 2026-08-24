package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketIndicatorCollectorTest {

  private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 6);

  @Test
  void collectMarksNormalDelayedAndMissingByLookbackDistance() {
    MarketIndicatorCollector collector =
        new MarketIndicatorCollector(
            List.of(
                fixedSource(MarketIndicatorType.KOSPI, BUSINESS_DATE),
                fixedSource(
                    MarketIndicatorType.KOSDAQ, BUSINESS_DATE.minusDays(5)),
                failingSource(MarketIndicatorType.USD_KRW),
                emptySource(MarketIndicatorType.US_TREASURY_10Y)));

    List<MarketIndicatorResult> results = collector.collect(BUSINESS_DATE);

    assertEquals(
        MarketIndicatorStatus.NORMAL,
        statusOf(results, MarketIndicatorType.KOSPI));
    assertEquals(
        MarketIndicatorStatus.DELAYED,
        statusOf(results, MarketIndicatorType.KOSDAQ));
    assertEquals(
        MarketIndicatorStatus.MISSING,
        statusOf(results, MarketIndicatorType.USD_KRW));
    assertEquals(
        MarketIndicatorStatus.MISSING,
        statusOf(results, MarketIndicatorType.US_TREASURY_10Y));
  }

  @Test
  void collectMarksExactlyToleranceDaysBehindAsNormal() {
    MarketIndicatorCollector collector =
        new MarketIndicatorCollector(
            List.of(
                fixedSource(
                    MarketIndicatorType.KOSPI,
                    BUSINESS_DATE.minusDays(MarketIndicatorCollector.HOLIDAY_TOLERANCE_DAYS))));

    List<MarketIndicatorResult> results = collector.collect(BUSINESS_DATE);

    assertEquals(MarketIndicatorStatus.NORMAL, statusOf(results, MarketIndicatorType.KOSPI));
  }

  @Test
  void collectMarksOneDayBeyondToleranceAsDelayed() {
    MarketIndicatorCollector collector =
        new MarketIndicatorCollector(
            List.of(
                fixedSource(
                    MarketIndicatorType.KOSPI,
                    BUSINESS_DATE.minusDays(
                        MarketIndicatorCollector.HOLIDAY_TOLERANCE_DAYS + 1))));

    List<MarketIndicatorResult> results = collector.collect(BUSINESS_DATE);

    assertEquals(MarketIndicatorStatus.DELAYED, statusOf(results, MarketIndicatorType.KOSPI));
  }

  @Test
  void collectMarksFutureDataAsOfAsMissing() {
    MarketIndicatorCollector collector =
        new MarketIndicatorCollector(
            List.of(fixedSource(MarketIndicatorType.KOSPI, BUSINESS_DATE.plusDays(1))));

    List<MarketIndicatorResult> results = collector.collect(BUSINESS_DATE);

    assertEquals(MarketIndicatorStatus.MISSING, statusOf(results, MarketIndicatorType.KOSPI));
  }

  @Test
  void collectUsesLatestStoredValueWhenExternalSourceReturnsEmpty() {
    DailyMarketIndicatorVo storedKosdaq =
        storedIndicator(
            MarketIndicatorType.KOSDAQ,
            BUSINESS_DATE.minusDays(3),
            new BigDecimal("840.89"));
    MarketIndicatorCollector collector =
        new MarketIndicatorCollector(
            List.of(emptySource(MarketIndicatorType.KOSDAQ)),
            new StubIndicatorMapper(storedKosdaq));

    MarketIndicatorResult result =
        resultOf(collector.collect(BUSINESS_DATE), MarketIndicatorType.KOSDAQ);

    assertEquals(MarketIndicatorStatus.DELAYED, result.status());
    assertEquals(new BigDecimal("840.89"), result.observation().observedValue());
    assertEquals(
        "금융위원회 지수시세정보 (최근 저장값)", result.observation().source());
  }

  @Test
  void collectUsesLatestStoredValueWhenExternalSourceThrows() {
    DailyMarketIndicatorVo storedTreasury =
        storedIndicator(
            MarketIndicatorType.US_TREASURY_10Y,
            BUSINESS_DATE.minusDays(1),
            new BigDecimal("4.69"));
    MarketIndicatorCollector collector =
        new MarketIndicatorCollector(
            List.of(failingSource(MarketIndicatorType.US_TREASURY_10Y)),
            new StubIndicatorMapper(storedTreasury));

    MarketIndicatorResult result =
        resultOf(collector.collect(BUSINESS_DATE), MarketIndicatorType.US_TREASURY_10Y);

    assertEquals(MarketIndicatorStatus.NORMAL, result.status());
    assertEquals(new BigDecimal("4.69"), result.observation().observedValue());
  }

  private MarketIndicatorStatus statusOf(
      List<MarketIndicatorResult> results, MarketIndicatorType type) {
    return resultOf(results, type).status();
  }

  private MarketIndicatorResult resultOf(
      List<MarketIndicatorResult> results, MarketIndicatorType type) {
    return results.stream()
        .filter(result -> result.type() == type)
        .findFirst()
        .orElseThrow();
  }

  private MarketIndicatorSource fixedSource(
      MarketIndicatorType type, LocalDate dataAsOf) {
    return source(
        type,
        Optional.of(
            new MarketIndicatorObservation(
                type,
                dataAsOf,
                "테스트 소스",
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ONE)));
  }

  private MarketIndicatorSource emptySource(MarketIndicatorType type) {
    return source(type, Optional.empty());
  }

  private MarketIndicatorSource source(
      MarketIndicatorType type,
      Optional<MarketIndicatorObservation> observation) {
    return new MarketIndicatorSource() {
      @Override
      public MarketIndicatorType type() {
        return type;
      }

      @Override
      public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
        return observation;
      }
    };
  }

  private MarketIndicatorSource failingSource(MarketIndicatorType type) {
    return new MarketIndicatorSource() {
      @Override
      public MarketIndicatorType type() {
        return type;
      }

      @Override
      public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
        throw new RuntimeException("소스 호출 실패");
      }
    };
  }

  private DailyMarketIndicatorVo storedIndicator(
      MarketIndicatorType type, LocalDate dataAsOf, BigDecimal observedValue) {
    return DailyMarketIndicatorVo.builder()
        .indicatorType(type)
        .dataAsOf(dataAsOf.atStartOfDay())
        .source(
            type == MarketIndicatorType.KOSDAQ
                ? "금융위원회 지수시세정보"
                : "FRED DGS10")
        .observedValue(observedValue)
        .status(MarketIndicatorStatus.NORMAL)
        .build();
  }

  private static class StubIndicatorMapper implements DailyMarketIndicatorMapper {

    private final DailyMarketIndicatorVo stored;

    private StubIndicatorMapper(DailyMarketIndicatorVo stored) {
      this.stored = stored;
    }

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
      throw new UnsupportedOperationException();
    }

    @Override
    public DailyMarketIndicatorVo findLatestAvailableBefore(
        MarketIndicatorType indicatorType, LocalDate beforeDate) {
      return stored.getIndicatorType() == indicatorType ? stored : null;
    }
  }
}
