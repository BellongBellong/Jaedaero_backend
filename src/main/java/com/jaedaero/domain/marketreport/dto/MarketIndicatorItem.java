package com.jaedaero.domain.marketreport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketIndicatorItem {
  private final MarketIndicatorType indicatorType;
  private final LocalDateTime dataAsOf;
  private final String source;
  private final BigDecimal observedValue;
  private final BigDecimal change;
  private final BigDecimal changeRate;
  private final MarketIndicatorStatus status;
}
