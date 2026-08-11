package com.jaedaero.domain.marketreport.service;

import java.time.LocalDate;
import java.util.List;

/** 시장 지표 4종 수집 결과를 제공한다. 로컬에서는 목데이터, 그 외 환경에서는 실제 수집기를 쓴다. */
public interface MarketIndicatorProvider {

  List<MarketIndicatorResult> collect(LocalDate businessDate);
}
