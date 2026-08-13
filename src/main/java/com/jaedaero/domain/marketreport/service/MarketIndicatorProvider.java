package com.jaedaero.domain.marketreport.service;

import java.time.LocalDate;
import java.util.List;

/** 실제 외부 provider에서 수집한 시장 지표 4종 결과를 제공한다. */
public interface MarketIndicatorProvider {

  List<MarketIndicatorResult> collect(LocalDate businessDate);
}
