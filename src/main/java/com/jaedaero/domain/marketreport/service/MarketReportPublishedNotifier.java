package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;

@FunctionalInterface
public interface MarketReportPublishedNotifier {
  void notifyPublished(DailyMarketReportVo report);

  static MarketReportPublishedNotifier noop() {
    return ignored -> {};
  }
}
