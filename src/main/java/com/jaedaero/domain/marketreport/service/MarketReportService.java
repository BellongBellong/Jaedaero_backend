package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.dto.TodayMarketIndicatorsResponse;

public interface MarketReportService {
  TodayMarketReportResponse getToday();

  TodayMarketIndicatorsResponse getTodayIndicators();
}
