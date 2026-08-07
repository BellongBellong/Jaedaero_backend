package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.aianalysis.llm.AiGenerationTask;
import com.jaedaero.domain.marketreport.dto.MarketCondition;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.llm.MarketReportNarrative;
import com.jaedaero.domain.marketreport.llm.MarketReportNarrativeGenerator;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class MarketReportGenerationService {

  static final String PROMPT_VERSION = "openai-chat-v1-daily-market-report";

  private final DailyMarketReportMapper reportMapper;
  private final DailyMarketIndicatorMapper indicatorMapper;
  private final MarketIndicatorCollector indicatorCollector;
  private final MarketReportNarrativeGenerator narrativeGenerator;
  private final Clock clock;

  public MarketReportGenerationService(
      DailyMarketReportMapper reportMapper,
      DailyMarketIndicatorMapper indicatorMapper,
      MarketIndicatorCollector indicatorCollector,
      MarketReportNarrativeGenerator narrativeGenerator,
      Clock clock) {
    this.reportMapper = reportMapper;
    this.indicatorMapper = indicatorMapper;
    this.indicatorCollector = indicatorCollector;
    this.narrativeGenerator = narrativeGenerator;
    this.clock = clock;
  }

  @Transactional
  public void generateForToday() {
    LocalDate reportDate = LocalDate.now(clock);
    if (reportMapper.countByReportDate(reportDate) > 0) {
      log.info("오늘자 시장 리포트가 이미 있어 배치를 건너뜁니다. reportDate={}", reportDate);
      return;
    }

    List<MarketIndicatorResult> indicators = indicatorCollector.collect(reportDate);
    MarketReportStatus reportStatus = deriveReportStatus(indicators);

    MarketCondition marketCondition;
    String content;
    MarketReportGenerationSource generationSource;
    try {
      MarketReportNarrative narrative =
          narrativeGenerator.generate(
              AiGenerationTask.DAILY_MARKET_REPORT.model(), prompt(indicators));
      marketCondition = narrative.marketCondition();
      content = narrative.content();
      generationSource = MarketReportGenerationSource.OPENAI;
    } catch (AiCoachNarrativeGenerationException exception) {
      log.warn(
          "오늘의 시장 리포트 서술 생성 실패, 템플릿으로 대체합니다. reason={}",
          exception.getMessage());
      marketCondition = MarketCondition.NEUTRAL;
      content = templateContent(indicators);
      generationSource = MarketReportGenerationSource.FALLBACK;
    }

    DailyMarketReportVo report =
        DailyMarketReportVo.builder()
            .reportDate(reportDate)
            .content(content)
            .marketCondition(marketCondition)
            .reportStatus(reportStatus)
            .generationSource(generationSource)
            .modelName(AiGenerationTask.DAILY_MARKET_REPORT.model().apiName())
            .promptVersion(PROMPT_VERSION)
            .validFrom(reportDate.atTime(18, 0))
            .validUntil(reportDate.plusDays(1).atTime(17, 59, 59))
            .build();
    reportMapper.upsert(report);
    if (report.getReportId() == null) {
      throw new IllegalStateException("저장된 시장 리포트 ID를 확인할 수 없습니다.");
    }

    for (MarketIndicatorResult result : indicators) {
      indicatorMapper.insert(toVo(report.getReportId(), result));
    }
  }

  private MarketReportStatus deriveReportStatus(
      List<MarketIndicatorResult> indicators) {
    boolean allNormal =
        indicators.size() == 4
            && indicators.stream()
                .allMatch(result -> result.status() == MarketIndicatorStatus.NORMAL);
    return allNormal ? MarketReportStatus.NORMAL : MarketReportStatus.PARTIAL;
  }

  private DailyMarketIndicatorVo toVo(
      long reportId, MarketIndicatorResult result) {
    DailyMarketIndicatorVo.DailyMarketIndicatorVoBuilder builder =
        DailyMarketIndicatorVo.builder()
            .reportId(reportId)
            .indicatorType(result.type())
            .status(result.status());
    if (result.observation() == null) {
      return builder
          .dataAsOf(LocalDateTime.now(clock))
          .source("N/A")
          .observedValue(BigDecimal.ZERO)
          .build();
    }
    return builder
        .dataAsOf(result.observation().dataAsOf().atStartOfDay())
        .source(result.observation().source())
        .observedValue(result.observation().observedValue())
        .changeValue(result.observation().change())
        .changeRate(result.observation().changeRate())
        .build();
  }

  private String prompt(List<MarketIndicatorResult> indicators) {
    StringBuilder builder = new StringBuilder("오늘의 시장 지표:\n");
    for (MarketIndicatorResult result : indicators) {
      builder.append("- ").append(result.type()).append(": ");
      if (result.observation() == null) {
        builder.append("데이터 누락\n");
        continue;
      }
      builder.append(result.observation().observedValue());
      if (result.observation().changeRate() != null) {
        builder
            .append(" (전일 대비 ")
            .append(result.observation().changeRate())
            .append("%)");
      }
      builder.append(" [").append(result.status()).append("]\n");
    }
    return builder
        .append("위 확정 수치만 근거로 marketCondition과 content를 작성하세요.")
        .toString();
  }

  private String templateContent(List<MarketIndicatorResult> indicators) {
    long missingCount =
        indicators.stream()
            .filter(result -> result.status() == MarketIndicatorStatus.MISSING)
            .count();
    return missingCount == 0
        ? "오늘의 시장 지표를 확인했습니다. 큰 변동 없이 안정적인 흐름입니다."
        : "일부 시장 지표를 일시적으로 확인하지 못했습니다. 확보된 지표 기준으로는 특이 동향이 없습니다.";
  }
}
