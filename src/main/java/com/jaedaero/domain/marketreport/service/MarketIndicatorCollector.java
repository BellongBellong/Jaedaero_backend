package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketIndicatorCollector {

  static final long HOLIDAY_TOLERANCE_DAYS = 2;

  private final List<MarketIndicatorSource> sources;

  public MarketIndicatorCollector(List<MarketIndicatorSource> sources) {
    this.sources = sources;
  }

  public List<MarketIndicatorResult> collect(LocalDate businessDate) {
    return sources.stream()
        .map(source -> collectOne(source, businessDate))
        .sorted(Comparator.comparing(MarketIndicatorResult::type))
        .toList();
  }

  private MarketIndicatorResult collectOne(
      MarketIndicatorSource source, LocalDate businessDate) {
    try {
      Optional<MarketIndicatorObservation> observation = source.fetch(businessDate);
      if (observation.isEmpty()) {
        return MarketIndicatorResult.missing(source.type());
      }
      long daysBehind =
          ChronoUnit.DAYS.between(observation.get().dataAsOf(), businessDate);
      MarketIndicatorStatus status =
          daysBehind <= HOLIDAY_TOLERANCE_DAYS
              ? MarketIndicatorStatus.NORMAL
              : MarketIndicatorStatus.DELAYED;
      return MarketIndicatorResult.of(observation.get(), status);
    } catch (RuntimeException exception) {
      log.warn(
          "지표 수집 실패, MISSING으로 기록합니다. type={}, reason={}",
          source.type(),
          exception.getMessage());
      return MarketIndicatorResult.missing(source.type());
    }
  }
}
