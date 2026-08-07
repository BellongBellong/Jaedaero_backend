package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.time.LocalDate;
import java.util.Optional;

/** 시장 지표 하나를 외부 공개 API에서 조회하는 전략. */
public interface MarketIndicatorSource {

  MarketIndicatorType type();

  Optional<MarketIndicatorObservation> fetch(LocalDate businessDate);
}
