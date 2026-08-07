package com.jaedaero.domain.marketreport.service.impl;

import com.jaedaero.domain.marketreport.dto.MarketCondition;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorItem;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import com.jaedaero.domain.marketreport.service.MilitaryProductSummaryFactory;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketReportServiceImpl implements MarketReportService {

  private final DailyMarketReportMapper reportMapper;
  private final DailyMarketIndicatorMapper indicatorMapper;
  private final MilitaryProductSummaryFactory militaryProductSummaryFactory;
  private final Clock clock;

  public MarketReportServiceImpl(
      DailyMarketReportMapper reportMapper,
      DailyMarketIndicatorMapper indicatorMapper,
      MilitaryProductSummaryFactory militaryProductSummaryFactory,
      Clock clock) {
    this.reportMapper = reportMapper;
    this.indicatorMapper = indicatorMapper;
    this.militaryProductSummaryFactory = militaryProductSummaryFactory;
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
    MarketCondition condition =
        report.getMarketCondition() == null
            ? MarketCondition.NEUTRAL
            : report.getMarketCondition();
    return TodayMarketReportResponse.builder()
        .reportId(report.getReportId())
        .reportDate(report.getReportDate())
        .reportStatus(
            stale ? MarketReportStatus.STALE : report.getReportStatus())
        .marketCondition(condition)
        .content(report.getContent())
        .recommendedAction(recommendedAction(condition))
        .indicators(indicatorRows.stream().map(this::toIndicatorItem).toList())
        .militaryProductSummary(militaryProductSummaryFactory.create())
        .generationSource(report.getGenerationSource())
        .modelName(report.getModelName())
        .promptVersion(report.getPromptVersion())
        .validFrom(report.getValidFrom())
        .validUntil(report.getValidUntil())
        .build();
  }

  private MarketIndicatorItem toIndicatorItem(DailyMarketIndicatorVo vo) {
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

  private String recommendedAction(MarketCondition condition) {
    return switch (condition) {
      case BULL -> "긍정적인 흐름이지만 무리한 추가 투자보다 계획한 배분을 유지해보세요.";
      case BEAR -> "신규 매수보다 관망을 검토해보세요.";
      case NEUTRAL -> "큰 변동이 없는 시기이니 기존 계획을 꾸준히 유지해보세요.";
    };
  }
}
