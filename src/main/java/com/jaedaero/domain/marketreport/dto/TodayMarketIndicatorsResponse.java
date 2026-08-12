package com.jaedaero.domain.marketreport.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 오늘의 시장 리포트 본문과 분리해 조회하는 시장 지표 카드 응답입니다. */
@Getter
@Builder
public class TodayMarketIndicatorsResponse {
  private final Long reportId;
  private final LocalDate reportDate;
  private final MarketReportStatus reportStatus;
  private final List<MarketIndicatorItem> indicators;
}
