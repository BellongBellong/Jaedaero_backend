package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.aianalysis.llm.OpenAiModel;
import com.jaedaero.domain.marketreport.dto.MarketCondition;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketReportGenerationServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(
          Instant.parse("2026-08-06T08:00:00Z"),
          ZoneId.of("Asia/Seoul"));

  @Test
  void generatePersistsNormalOpenAiReportAndFourIndicators() {
    RecordingReportMapper reports = new RecordingReportMapper();
    RecordingIndicatorMapper indicators = new RecordingIndicatorMapper();

    new MarketReportGenerationService(
            reports,
            indicators,
            collector(false, false),
            new SucceedingNarrativeGenerator(),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(1, reports.upserted.size());
    assertEquals(MarketReportStatus.NORMAL, reports.upserted.get(0).getReportStatus());
    assertEquals(
        MarketReportGenerationSource.OPENAI,
        reports.upserted.get(0).getGenerationSource());
    assertEquals("gpt-5-nano", reports.upserted.get(0).getModelName());
    assertEquals(LocalDateTime.of(2026, 8, 6, 18, 0), reports.upserted.get(0).getValidFrom());
    assertEquals(4, indicators.inserted.size());
  }

  @Test
  void generateMarksPartialWhenIndicatorIsMissing() {
    RecordingReportMapper reports = new RecordingReportMapper();

    new MarketReportGenerationService(
            reports,
            new RecordingIndicatorMapper(),
            collector(true, false),
            new SucceedingNarrativeGenerator(),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(MarketReportStatus.PARTIAL, reports.upserted.get(0).getReportStatus());
  }

  @Test
  void generateMarksPartialAndPersistsDelayedWhenIndicatorIsDelayed() {
    RecordingReportMapper reports = new RecordingReportMapper();
    RecordingIndicatorMapper indicators = new RecordingIndicatorMapper();

    new MarketReportGenerationService(
            reports,
            indicators,
            collector(false, true),
            new SucceedingNarrativeGenerator(),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(MarketReportStatus.PARTIAL, reports.upserted.get(0).getReportStatus());
    DailyMarketIndicatorVo kosdaq =
        indicators.inserted.stream()
            .filter(vo -> vo.getIndicatorType() == MarketIndicatorType.KOSDAQ)
            .findFirst()
            .orElseThrow();
    assertEquals(
        com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus.DELAYED,
        kosdaq.getStatus());
  }

  @Test
  void generateFallsBackWhenOpenAiFails() {
    RecordingReportMapper reports = new RecordingReportMapper();

    new MarketReportGenerationService(
            reports,
            new RecordingIndicatorMapper(),
            collector(false, false),
            (model, prompt) -> {
              throw new AiCoachNarrativeGenerationException("테스트 실패");
            },
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(
        MarketReportGenerationSource.FALLBACK,
        reports.upserted.get(0).getGenerationSource());
    assertTrue(reports.upserted.get(0).getContent().length() > 0);
  }

  @Test
  void generateIsIdempotentWhenReportAlreadyExists() {
    RecordingReportMapper reports = new RecordingReportMapper();
    reports.existing.put(LocalDate.of(2026, 8, 6), 1);
    SucceedingNarrativeGenerator generator = new SucceedingNarrativeGenerator();

    new MarketReportGenerationService(
            reports,
            new RecordingIndicatorMapper(),
            collector(false, false),
            generator,
            FIXED_CLOCK)
        .generateForToday();

    assertTrue(reports.upserted.isEmpty());
    assertEquals(0, generator.calls);
  }

  private MarketIndicatorCollector collector(
      boolean missingKosdaq, boolean delayedKosdaq) {
    LocalDate reportDate = LocalDate.of(2026, 8, 6);
    return new MarketIndicatorCollector(
        List.of(
            source(MarketIndicatorType.KOSPI, reportDate),
            missingKosdaq
                ? emptySource(MarketIndicatorType.KOSDAQ)
                : source(
                    MarketIndicatorType.KOSDAQ,
                    delayedKosdaq ? reportDate.minusDays(5) : reportDate),
            source(MarketIndicatorType.US_TREASURY_10Y, reportDate),
            source(MarketIndicatorType.USD_KRW, reportDate)));
  }

  private MarketIndicatorSource source(
      MarketIndicatorType type, LocalDate dataAsOf) {
    return new MarketIndicatorSource() {
      @Override
      public MarketIndicatorType type() {
        return type;
      }

      @Override
      public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
        return Optional.of(
            new MarketIndicatorObservation(
                type,
                dataAsOf,
                "테스트 소스",
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ONE));
      }
    };
  }

  private MarketIndicatorSource emptySource(MarketIndicatorType type) {
    return new MarketIndicatorSource() {
      @Override
      public MarketIndicatorType type() {
        return type;
      }

      @Override
      public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
        return Optional.empty();
      }
    };
  }

  private static class SucceedingNarrativeGenerator
      implements MarketReportNarrativeGenerator {
    private int calls;

    @Override
    public MarketReportNarrative generate(OpenAiModel model, String prompt) {
      calls++;
      return new MarketReportNarrative(MarketCondition.NEUTRAL, "테스트 요약");
    }
  }

  private static class RecordingReportMapper implements DailyMarketReportMapper {
    private final List<DailyMarketReportVo> upserted = new ArrayList<>();
    private final Map<LocalDate, Integer> existing = new HashMap<>();

    @Override
    public int upsert(DailyMarketReportVo report) {
      report.setReportId((long) upserted.size() + 1);
      upserted.add(report);
      return 1;
    }

    @Override
    public DailyMarketReportVo findActiveAt(LocalDateTime now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DailyMarketReportVo findLatest() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int countByReportDate(LocalDate reportDate) {
      return existing.getOrDefault(reportDate, 0);
    }

    @Override
    public int claimReportDate(
        LocalDate reportDate, LocalDateTime validFrom, LocalDateTime validUntil) {
      if (existing.getOrDefault(reportDate, 0) > 0) {
        return 0;
      }
      existing.put(reportDate, 1);
      return 1;
    }
  }

  private static class RecordingIndicatorMapper
      implements DailyMarketIndicatorMapper {
    private final List<DailyMarketIndicatorVo> inserted = new ArrayList<>();

    @Override
    public int insert(DailyMarketIndicatorVo indicator) {
      inserted.add(indicator);
      return 1;
    }

    @Override
    public List<DailyMarketIndicatorVo> findByReportId(long reportId) {
      throw new UnsupportedOperationException();
    }
  }
}
