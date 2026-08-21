package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportSourceVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.util.List;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 외부 수집·생성 결과를 짧은 원자적 저장 구간에서 리포트와 함께 반영한다. */
@Component
public class MarketReportPersistenceService {

  private final DailyMarketReportMapper reportMapper;
  private final DailyMarketIndicatorMapper indicatorMapper;
  private final DailyMarketReportSourceMapper sourceMapper;
  private final MarketReportPublishedNotifier publishedNotifier;

  @Autowired
  public MarketReportPersistenceService(
      DailyMarketReportMapper reportMapper,
      DailyMarketIndicatorMapper indicatorMapper,
      DailyMarketReportSourceMapper sourceMapper,
      MarketReportPublishedNotifier publishedNotifier) {
    this.reportMapper = reportMapper;
    this.indicatorMapper = indicatorMapper;
    this.sourceMapper = sourceMapper;
    this.publishedNotifier = publishedNotifier;
  }

  /** 알림과 무관한 단위 테스트를 위한 생성자입니다. */
  public MarketReportPersistenceService(
      DailyMarketReportMapper reportMapper,
      DailyMarketIndicatorMapper indicatorMapper,
      DailyMarketReportSourceMapper sourceMapper) {
    this(reportMapper, indicatorMapper, sourceMapper, MarketReportPublishedNotifier.noop());
  }

  @Transactional
  public void persist(
      DailyMarketReportVo report,
      List<MarketIndicatorResult> indicators,
      List<MarketReportSourceItem> sources) {
    reportMapper.upsert(report);
    Long reportId = report == null ? null : report.getReportId();
    if (reportId == null || reportId <= 0) {
      throw new IllegalStateException("저장된 시장 리포트 ID를 확인할 수 없습니다.");
    }

    indicatorMapper.deleteByReportId(reportId);
    sourceMapper.deleteByReportId(reportId);
    for (MarketIndicatorResult result : indicators) {
      indicatorMapper.insert(toIndicatorVo(reportId, result));
    }
    for (int index = 0; index < sources.size(); index++) {
      sourceMapper.insert(toSourceVo(reportId, index + 1, sources.get(index)));
    }
    publishedNotifier.notifyPublished(report);
  }

  /** 기존 본문·출처는 유지하고, 해당 날짜의 지표 스냅샷과 전체 상태만 갱신합니다. */
  @Transactional
  public void refreshIndicators(
      LocalDate reportDate,
      List<MarketIndicatorResult> indicators,
      MarketReportStatus reportStatus) {
    DailyMarketReportVo report = reportMapper.findByReportDateForUpdate(reportDate);
    if (report == null || report.getReportId() == null) {
      throw new IllegalStateException("오늘자 시장 리포트를 찾을 수 없습니다.");
    }
    long reportId = report.getReportId();
    indicatorMapper.deleteByReportId(reportId);
    for (MarketIndicatorResult result : indicators) {
      indicatorMapper.insert(toIndicatorVo(reportId, result));
    }
    reportMapper.updateReportStatus(reportId, reportStatus.name());
  }

  private DailyMarketIndicatorVo toIndicatorVo(long reportId, MarketIndicatorResult result) {
    DailyMarketIndicatorVo.DailyMarketIndicatorVoBuilder builder =
        DailyMarketIndicatorVo.builder()
            .reportId(reportId)
            .indicatorType(result.type())
            .status(result.status());
    if (result.observation() == null) {
      return builder.dataAsOf(null).source("N/A").observedValue(null).build();
    }
    return builder
        .dataAsOf(result.observation().dataAsOf().atStartOfDay())
        .source(result.observation().source())
        .observedValue(result.observation().observedValue())
        .changeValue(result.observation().change())
        .changeRate(result.observation().changeRate())
        .build();
  }

  private DailyMarketReportSourceVo toSourceVo(
      long reportId, int sourceOrder, MarketReportSourceItem source) {
    return DailyMarketReportSourceVo.builder()
        .reportId(reportId)
        .sourceOrder(sourceOrder)
        .title(source.getTitle())
        .url(source.getUrl())
        .build();
  }
}
