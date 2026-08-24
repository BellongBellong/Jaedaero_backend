package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class MarketIndicatorCollector implements MarketIndicatorProvider {

  static final long HOLIDAY_TOLERANCE_DAYS = 2;
  private static final String STORED_VALUE_SOURCE_SUFFIX = " (최근 저장값)";

  private final List<MarketIndicatorSource> sources;
  private final DailyMarketIndicatorMapper indicatorMapper;

  @Autowired
  public MarketIndicatorCollector(
      List<MarketIndicatorSource> sources,
      DailyMarketIndicatorMapper indicatorMapper) {
    this.sources = sources;
    this.indicatorMapper = indicatorMapper;
  }

  MarketIndicatorCollector(List<MarketIndicatorSource> sources) {
    this(sources, null);
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
    Optional<MarketIndicatorObservation> observation;
    try {
      observation = source.fetch(businessDate);
    } catch (RuntimeException exception) {
      log.warn(
          "외부 지표 수집 실패, 최근 저장값을 확인합니다. type={}, reason={}",
          source.type(),
          exception.getMessage());
      observation = Optional.empty();
    }

    if (observation.isEmpty()) {
      observation = latestStoredObservation(source.type(), businessDate);
    }
    if (observation.isEmpty()) {
      log.warn("사용 가능한 지표가 없어 MISSING으로 기록합니다. type={}", source.type());
      return MarketIndicatorResult.missing(source.type());
    }

    long daysBehind = ChronoUnit.DAYS.between(observation.get().dataAsOf(), businessDate);
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
  }

  private Optional<MarketIndicatorObservation> latestStoredObservation(
      MarketIndicatorType type, LocalDate businessDate) {
    if (indicatorMapper == null) {
      return Optional.empty();
    }
    try {
      DailyMarketIndicatorVo stored =
          indicatorMapper.findLatestAvailableBefore(type, businessDate);
      if (stored == null) {
        return Optional.empty();
      }
      String source = stored.getSource();
      if (!source.endsWith(STORED_VALUE_SOURCE_SUFFIX)) {
        source += STORED_VALUE_SOURCE_SUFFIX;
      }
      log.info(
          "최근 저장된 지표를 사용합니다. type={}, dataAsOf={}",
          type,
          stored.getDataAsOf());
      return Optional.of(
          new MarketIndicatorObservation(
              type,
              stored.getDataAsOf().toLocalDate(),
              source,
              stored.getObservedValue(),
              stored.getChangeValue(),
              stored.getChangeRate()));
    } catch (RuntimeException exception) {
      log.warn(
          "최근 저장 지표 조회 실패, MISSING으로 기록합니다. type={}, reason={}",
          type,
          exception.getMessage());
      return Optional.empty();
    }
  }
}
