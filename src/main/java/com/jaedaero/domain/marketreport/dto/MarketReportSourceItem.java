package com.jaedaero.domain.marketreport.dto;

import lombok.Builder;
import lombok.Getter;

/** Gemini가 실제 사용했다고 반환한 Finnhub 시장 뉴스 출처. */
@Getter
@Builder
public class MarketReportSourceItem {
  private final String title;
  private final String url;
}
