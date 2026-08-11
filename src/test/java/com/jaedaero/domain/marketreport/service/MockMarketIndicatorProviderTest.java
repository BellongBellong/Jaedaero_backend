package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MockMarketIndicatorProviderTest {

  @Test
  void collect_returnsFourNormalIndicatorsForBusinessDate() {
    LocalDate businessDate = LocalDate.of(2026, 8, 7);
    MockMarketIndicatorProvider provider = new MockMarketIndicatorProvider();

    List<MarketIndicatorResult> results = provider.collect(businessDate);

    assertEquals(4, results.size());
    assertEquals(
        Set.of(
            MarketIndicatorType.KOSPI,
            MarketIndicatorType.KOSDAQ,
            MarketIndicatorType.USD_KRW,
            MarketIndicatorType.US_TREASURY_10Y),
        results.stream().map(MarketIndicatorResult::type).collect(java.util.stream.Collectors.toSet()));
    assertTrue(results.stream().allMatch(r -> r.status() == MarketIndicatorStatus.NORMAL));
    assertTrue(results.stream().allMatch(r -> r.observation().dataAsOf().equals(businessDate)));
  }
}
