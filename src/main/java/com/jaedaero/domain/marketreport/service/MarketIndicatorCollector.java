package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class MarketIndicatorCollector implements MarketIndicatorProvider {

  static final long HOLIDAY_TOLERANCE_DAYS = 2;

  private final List<MarketIndicatorSource> sources;

  public MarketIndicatorCollector(List<MarketIndicatorSource> sources) {
    this.sources = sources;
  }

  @Override
  public List<MarketIndicatorResult> collect(LocalDate businessDate) {
    return sources.stream()
        .map(source -> collectOne(source, businessDate))
        .sorted(Comparator.comparingInt(result -> result.type().ordinal()))
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
      if (daysBehind < 0) {
        log.warn(
            "지표 기준일이 미래입니다, MISSING으로 기록합니다. type={}, dataAsOf={}, businessDate={}",
            source.type(),
            observation.get().dataAsOf(),
            businessDate);
        return MarketIndicatorResult.missing(source.type());
      }
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
