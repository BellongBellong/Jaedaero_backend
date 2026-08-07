package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
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

  private MarketIndicatorStatus statusOf(
      List<MarketIndicatorResult> results, MarketIndicatorType type) {
    return results.stream()
        .filter(result -> result.type() == type)
        .findFirst()
        .orElseThrow()
        .status();
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
}
