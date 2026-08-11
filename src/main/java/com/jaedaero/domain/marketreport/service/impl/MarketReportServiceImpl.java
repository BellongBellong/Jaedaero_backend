package com.jaedaero.domain.marketreport.service.impl;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorItem;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportSourceVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketReportServiceImpl implements MarketReportService {

  private final DailyMarketReportMapper reportMapper;
  private final DailyMarketIndicatorMapper indicatorMapper;
  private final DailyMarketReportSourceMapper sourceMapper;
  private final Clock clock;

  public MarketReportServiceImpl(
      DailyMarketReportMapper reportMapper,
      DailyMarketIndicatorMapper indicatorMapper,
      DailyMarketReportSourceMapper sourceMapper,
      Clock clock) {
    this.reportMapper = reportMapper;
    this.indicatorMapper = indicatorMapper;
    this.sourceMapper = sourceMapper;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public TodayMarketReportResponse getToday() {
    LocalDateTime now = LocalDateTime.now(clock);
    DailyMarketReportVo report = reportMapper.findActiveAt(now);
    boolean stale = false;
    if (report == null) {
      report = reportMapper.findLatest();
      if (report == null) {
        throw new MarketReportException(
            MarketReportErrorCode.NOT_FOUND,
            "오늘의 시장 리포트가 아직 생성되지 않았습니다.");
      }
      stale = true;
    }

    List<DailyMarketIndicatorVo> indicatorRows =
        indicatorMapper.findByReportId(report.getReportId());
    List<DailyMarketReportSourceVo> sourceRows =
        sourceMapper.findByReportId(report.getReportId());
    return TodayMarketReportResponse.builder()
        .reportId(report.getReportId())
        .reportDate(report.getReportDate())
        .reportStatus(
            stale
                ? MarketReportStatus.STALE
                : report.getReportStatus() == null
                    ? MarketReportStatus.PARTIAL
                    : report.getReportStatus())
        .title(report.getTitle())
        .summary(report.getSummary())
        .content(report.getContent())
        .indicators(toIndicatorItems(indicatorRows))
        .sources(sourceRows.stream().map(this::toSourceItem).toList())
        .generationSource(report.getGenerationSource())
        .modelName(report.getModelName())
        .promptVersion(report.getPromptVersion())
        .validFrom(report.getValidFrom())
        .validUntil(report.getValidUntil())
        .build();
  }

  private List<MarketIndicatorItem> toIndicatorItems(List<DailyMarketIndicatorVo> rows) {
    Map<MarketIndicatorType, DailyMarketIndicatorVo> rowsByType =
        new EnumMap<>(MarketIndicatorType.class);
    for (DailyMarketIndicatorVo row : rows) {
      if (row.getIndicatorType() != null) {
        rowsByType.putIfAbsent(row.getIndicatorType(), row);
      }
    }
    return Arrays.stream(MarketIndicatorType.values())
        .map(type -> toIndicatorItem(type, rowsByType.get(type)))
        .toList();
  }

  private MarketIndicatorItem toIndicatorItem(
      MarketIndicatorType type, DailyMarketIndicatorVo vo) {
    if (vo == null) {
      return MarketIndicatorItem.builder()
          .indicatorType(type)
          .source("N/A")
          .status(MarketIndicatorStatus.MISSING)
          .build();
    }
    return MarketIndicatorItem.builder()
        .indicatorType(vo.getIndicatorType())
        .dataAsOf(vo.getDataAsOf())
        .source(vo.getSource())
        .observedValue(vo.getObservedValue())
        .change(vo.getChangeValue())
        .changeRate(vo.getChangeRate())
        .status(vo.getStatus())
        .build();
  }

  private MarketReportSourceItem toSourceItem(DailyMarketReportSourceVo vo) {
    return MarketReportSourceItem.builder().title(vo.getTitle()).url(vo.getUrl()).build();
  }
}
