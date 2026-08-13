package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** 운영 관리자 요청으로 현재 또는 최신 PARTIAL 리포트의 시장 지표만 다시 수집합니다. Gemini 본문은 다시 생성하지 않습니다. */
@Service
public class MarketReportIndicatorRefreshService {

  private final DailyMarketReportMapper reportMapper;
  private final MarketIndicatorProvider indicatorProvider;
  private final MarketReportPersistenceService persistenceService;
  private final Clock clock;

  public MarketReportIndicatorRefreshService(
      DailyMarketReportMapper reportMapper,
      MarketIndicatorProvider indicatorProvider,
      MarketReportPersistenceService persistenceService,
      Clock clock) {
    this.reportMapper = reportMapper;
    this.indicatorProvider = indicatorProvider;
    this.persistenceService = persistenceService;
    this.clock = clock;
  }

  public void refreshToday() {
    DailyMarketReportVo report = reportMapper.findActiveAt(LocalDateTime.now(clock));
    if (report == null) {
      report = reportMapper.findLatest();
    }
    if (report == null) {
      throw new MarketReportException(
          MarketReportErrorCode.NOT_FOUND, "재수집할 시장 리포트가 없습니다.");
    }
    if (report.getReportStatus() != MarketReportStatus.PARTIAL) {
      throw new MarketReportException(
          MarketReportErrorCode.INDICATOR_REFRESH_NOT_AVAILABLE,
          "지표 재수집은 PARTIAL 상태의 현재 또는 최신 시장 리포트에서만 실행할 수 있습니다.");
    }
    List<MarketIndicatorResult> indicators = indicatorProvider.collect(report.getReportDate());
    persistenceService.refreshIndicators(
        report.getReportDate(), indicators, status(report, indicators));
  }

  private MarketReportStatus status(
      DailyMarketReportVo report, List<MarketIndicatorResult> indicators) {
    boolean allNormal =
        indicators.size() == MarketIndicatorType.values().length
            && indicators.stream()
                .allMatch(result -> result.status() == MarketIndicatorStatus.NORMAL);
    return allNormal && report.getGenerationSource() == MarketReportGenerationSource.GEMINI
        ? MarketReportStatus.NORMAL
        : MarketReportStatus.PARTIAL;
  }
}
