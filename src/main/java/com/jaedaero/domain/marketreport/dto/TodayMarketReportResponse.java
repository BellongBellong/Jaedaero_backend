package com.jaedaero.domain.marketreport.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodayMarketReportResponse {
  private final Long reportId;
  private final LocalDate reportDate;
  private final MarketReportStatus reportStatus;
  private final MarketCondition marketCondition;
  private final String content;
  private final String recommendedAction;
  private final List<MarketIndicatorItem> indicators;
  private final MilitaryProductSummaryItem militaryProductSummary;
  private final MarketReportGenerationSource generationSource;
  private final String modelName;
  private final String promptVersion;
  private final LocalDateTime validFrom;
  private final LocalDateTime validUntil;
}
