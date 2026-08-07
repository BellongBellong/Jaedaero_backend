package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;

/** 수집 결과와 신선도 상태. MISSING이면 observation은 null이다. */
public record MarketIndicatorResult(
    MarketIndicatorType type,
    MarketIndicatorStatus status,
    MarketIndicatorObservation observation) {

  public static MarketIndicatorResult missing(MarketIndicatorType type) {
    return new MarketIndicatorResult(type, MarketIndicatorStatus.MISSING, null);
  }

  public static MarketIndicatorResult of(
      MarketIndicatorObservation observation, MarketIndicatorStatus status) {
    return new MarketIndicatorResult(observation.type(), status, observation);
  }
}
