package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;

public interface MarketReportService {
  TodayMarketReportResponse getToday();
}
