package com.jaedaero.domain.marketreport.vo;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMarketIndicatorVo {
  private Long indicatorId;
  private Long reportId;
  private MarketIndicatorType indicatorType;
  private LocalDateTime dataAsOf;
  private String source;
  private BigDecimal observedValue;
  private BigDecimal changeValue;
  private BigDecimal changeRate;
  private MarketIndicatorStatus status;
  private LocalDateTime createdAt;
}
