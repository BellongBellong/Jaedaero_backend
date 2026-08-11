package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 외부 API에서 수집한 지표 원본값. */
public record MarketIndicatorObservation(
    MarketIndicatorType type,
    LocalDate dataAsOf,
    String source,
    BigDecimal observedValue,
    BigDecimal change,
    BigDecimal changeRate) {}
