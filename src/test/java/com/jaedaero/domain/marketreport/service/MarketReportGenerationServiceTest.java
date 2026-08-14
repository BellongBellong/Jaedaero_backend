package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import com.jaedaero.domain.marketreport.dto.MarketReportStatus;
import com.jaedaero.domain.marketreport.llm.GeminiMarketReportNarrativeGenerator;
import com.jaedaero.domain.marketreport.llm.MarketReportNarrative;
import com.jaedaero.domain.marketreport.llm.MarketReportNarrativeGenerator;
import com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper;
import com.sun.net.httpserver.HttpServer;
import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportSourceVo;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.math.BigDecimal;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

class MarketReportGenerationServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-06T08:00:00Z"), ZoneId.of("Asia/Seoul"));
  private static final String VALID_CONTENT =
      "오늘 확인된 시장 지표와 인용 가능한 뉴스의 사실을 중심으로 설명합니다. "
          + "확인된 내용과 아직 확정할 수 없는 내용을 구분하고, 특정 자산의 가격이나 수익을 예측하지 않습니다. "
          + "각 문장은 검색 결과의 인용 범위 안에서만 정리하며, 자료 사이에 차이가 있으면 그 불확실성을 함께 표시합니다. "
          + "이 리포트는 KOSPI, KOSDAQ, 미국채 10년물, 원/달러 환율의 기준일과 수집 상태를 함께 고려합니다. "
          + "수치의 의미는 해당 지표의 관측 시점과 제공처를 기준으로 읽어야 하며, 당일 장이 열리지 않은 경우에는 최근 기준일을 구분합니다. "
          + "뉴스의 배경과 시장 반응을 설명할 때에도 출처에 없는 원인이나 전망을 덧붙이지 않습니다. "
          + "따라서 독자는 본문과 출처 목록을 함께 확인하고, 인용되지 않은 판단은 이 문서의 결론으로 해석하지 않아야 합니다. ";

  @Test
  void keepsExternalGenerationOutsideTransactionAndPersistenceAtomic() throws NoSuchMethodException {
    Method generate = MarketReportGenerationService.class.getMethod("generateForToday");
    Method localRetry =
        MarketReportGenerationService.class.getMethod("generateForTodayForLocalRetry");
    Method persist =
        MarketReportPersistenceService.class.getMethod(
            "persist", DailyMarketReportVo.class, List.class, List.class);

    assertNull(generate.getAnnotation(Transactional.class));
    assertNull(localRetry.getAnnotation(Transactional.class));
    assertNotNull(persist.getAnnotation(Transactional.class));
  }

  @Test
  void generatePersistsNormalGeminiReportFourIndicatorsAndSources() {
    RecordingReportMapper reports = new RecordingReportMapper();
    RecordingIndicatorMapper indicators = new RecordingIndicatorMapper();
    RecordingSourceMapper sources = new RecordingSourceMapper();
    SucceedingNarrativeGenerator generator = new SucceedingNarrativeGenerator();

    new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(false, false),
            ignored -> List.of(),
            generator,
            new MarketReportPersistenceService(reports, indicators, sources),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(1, reports.upserted.size());
    assertEquals(MarketReportStatus.NORMAL, reports.upserted.get(0).getReportStatus());
    assertEquals(
        MarketReportGenerationSource.GEMINI,
        reports.upserted.get(0).getGenerationSource());
    assertEquals("인용으로 확인한 테스트 시장 리포트", reports.upserted.get(0).getTitle());
    assertEquals("확인된 사실을 한 줄로 정리한 테스트 요약", reports.upserted.get(0).getSummary());
    assertEquals("gemini-3.6-flash", reports.upserted.get(0).getModelName());
    assertEquals(LocalDateTime.of(2026, 8, 6, 18, 0), reports.upserted.get(0).getValidFrom());
    assertEquals(4, indicators.inserted.size());
    assertEquals(2, sources.inserted.size());
    assertEquals(1, sources.inserted.get(0).getSourceOrder());
    assertTrue(generator.prompt.contains("한국 표준시(KST, Asia/Seoul) 기준 서비스 날짜: 2026-08-06"));
    assertTrue(generator.prompt.contains("최신성·시장 관련성·매체 다양성으로 선별한 뉴스 후보"));
    assertTrue(generator.prompt.contains("Finnhub Market News"));
    assertTrue(generator.prompt.contains("지표 값·등락·상태를 반복하거나 별도로 해설하지 마세요"));
    assertTrue(generator.prompt.contains("핵심 사건, 시장 반응과 주요 쟁점"));
    assertTrue(generator.prompt.contains("title, summary, content, sourceIds 필드"));
    assertTrue(generator.prompt.contains("한국어 300자 이상의 본문"));
    assertTrue(generator.prompt.contains("양의 정수"));
    assertTrue(generator.prompt.contains("[\"N1\", \"N3\"]"));
  }

  @Test
  void generateMarksPartialWhenIndicatorIsMissing() {
    RecordingReportMapper reports = new RecordingReportMapper();

    new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(true, false),
            ignored -> List.of(),
            new SucceedingNarrativeGenerator(),
            new MarketReportPersistenceService(
                reports, new RecordingIndicatorMapper(), new RecordingSourceMapper()),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(MarketReportStatus.PARTIAL, reports.upserted.get(0).getReportStatus());
  }

  @Test
  void generateMarksPartialAndPersistsDelayedWhenIndicatorIsDelayed() {
    RecordingReportMapper reports = new RecordingReportMapper();
    RecordingIndicatorMapper indicators = new RecordingIndicatorMapper();

    new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(false, true),
            ignored -> List.of(),
            new SucceedingNarrativeGenerator(),
            new MarketReportPersistenceService(reports, indicators, new RecordingSourceMapper()),
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
  void generateFallsBackWithoutInventedNewsWhenGeminiFailsAndNeverRetriesForDate() {
    RecordingReportMapper reports = new RecordingReportMapper();
    RecordingSourceMapper sources = new RecordingSourceMapper();
    FailingNarrativeGenerator generator = new FailingNarrativeGenerator();
    MarketReportGenerationService service =
        new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(false, false),
            ignored -> List.of(),
            generator,
            new MarketReportPersistenceService(
                reports, new RecordingIndicatorMapper(), sources),
            FIXED_CLOCK);

    service.generateForToday();
    service.generateForToday();

    assertEquals(1, generator.calls);
    assertEquals(
        MarketReportGenerationSource.FALLBACK,
        reports.upserted.get(0).getGenerationSource());
    assertEquals(MarketReportStatus.PARTIAL, reports.upserted.get(0).getReportStatus());
    assertEquals(MarketReportGenerationService.FALLBACK_CONTENT, reports.upserted.get(0).getContent());
    assertTrue(sources.inserted.isEmpty());
  }

  @Test
  void generateTreatsSingleCitationResponseAsFallback() {
    RecordingReportMapper reports = new RecordingReportMapper();

    new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(false, false),
            ignored -> List.of(),
            prompt ->
                new MarketReportNarrative(
                    "오늘의 AI 시장 리포트",
                    "출처가 하나뿐이면 정상 리포트로 저장하지 않습니다.",
                    VALID_CONTENT,
                    List.of(
                        MarketReportSourceItem.builder()
                            .title("단일 출처")
                            .url("https://example.com/only-source")
                            .build())),
            new MarketReportPersistenceService(
                reports, new RecordingIndicatorMapper(), new RecordingSourceMapper()),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(MarketReportGenerationSource.FALLBACK, reports.upserted.get(0).getGenerationSource());
    assertTrue(
        reports.upserted.get(0).getContent().codePointCount(0, reports.upserted.get(0).getContent().length())
            >= MarketReportGenerationService.MINIMUM_CONTENT_CHAR_COUNT);
  }

  @Test
  void generateFallsBackWhenTitleExceedsDatabaseLength() {
    RecordingReportMapper reports = new RecordingReportMapper();

    new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(false, false),
            ignored -> List.of(),
            prompt ->
                new MarketReportNarrative(
                    "가".repeat(201),
                    "저장 길이를 확인하는 요약",
                    VALID_CONTENT,
                    List.of(
                        MarketReportSourceItem.builder()
                            .title("첫 출처")
                            .url("https://example.com/first")
                            .build(),
                        MarketReportSourceItem.builder()
                            .title("두 번째 출처")
                            .url("https://example.org/second")
                            .build())),
            new MarketReportPersistenceService(
                reports, new RecordingIndicatorMapper(), new RecordingSourceMapper()),
            FIXED_CLOCK)
        .generateForToday();

    assertEquals(
        MarketReportGenerationSource.FALLBACK, reports.upserted.get(0).getGenerationSource());
  }

  @Test
  void localRetryRegeneratesFallbackAndCleansRelatedRowsButNeverRetriesGemini() {
    RecordingReportMapper reports = new RecordingReportMapper();
    RecordingIndicatorMapper indicators = new RecordingIndicatorMapper();
    RecordingSourceMapper sources = new RecordingSourceMapper();
    AtomicInteger calls = new AtomicInteger();
    MarketReportNarrativeGenerator generator =
        prompt -> {
          if (calls.incrementAndGet() == 1) {
            throw new AiCoachNarrativeGenerationException("HTTP 429: quota exceeded");
          }
          return new MarketReportNarrative(
              "재시험으로 확인한 시장 리포트",
              "재시험에서 확인된 사실을 한 줄로 정리했습니다.",
              VALID_CONTENT,
              List.of(
                  MarketReportSourceItem.builder()
                      .title("재시험 첫 출처")
                      .url("https://example.com/retry-first")
                      .build(),
                  MarketReportSourceItem.builder()
                      .title("재시험 두 번째 출처")
                      .url("https://example.org/retry-second")
                      .build()));
        };
    MarketReportGenerationService service =
        new MarketReportGenerationService(
            new MarketReportClaimService(reports),
            collector(false, false),
            testNewsProvider(),
            generator,
            new MarketReportPersistenceService(reports, indicators, sources),
            FIXED_CLOCK);

    service.generateForToday();
    service.generateForToday();
    service.generateForTodayForLocalRetry();
    service.generateForTodayForLocalRetry();

    assertEquals(2, calls.get());
    assertEquals(2, reports.upserted.size());
    assertEquals(MarketReportGenerationSource.GEMINI, reports.upserted.get(1).getGenerationSource());
    assertEquals(4, indicators.inserted.size());
    assertEquals(2, sources.inserted.size());
    assertEquals("https://example.com/retry-first", sources.inserted.get(0).getUrl());
  }

  @Test
  void sameDateGenerationMakesOneGeminiHttpRequestAndPersistsSources() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    AtomicInteger requestCount = new AtomicInteger();
    String responseBody = structuredGeminiResponse();
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange -> {
          requestCount.incrementAndGet();
          exchange.getRequestBody().readAllBytes();
          respond(exchange, 200, responseBody);
        });
    server.start();
    try {
      RecordingReportMapper reports = new RecordingReportMapper();
      RecordingSourceMapper sources = new RecordingSourceMapper();
      MarketReportGenerationService service =
          new MarketReportGenerationService(
              new MarketReportClaimService(reports),
              collector(false, false),
              testNewsProvider(),
              new GeminiMarketReportNarrativeGenerator(
                  new RestTemplate(),
                  new ObjectMapper(),
                  "test-gemini-key",
                  "http://localhost:"
                      + server.getAddress().getPort()
                      + "/v1beta/models/gemini-3.6-flash:generateContent"),
              new MarketReportPersistenceService(
                  reports, new RecordingIndicatorMapper(), sources),
              FIXED_CLOCK);

      service.generateForToday();
      service.generateForToday();

      assertEquals(1, requestCount.get());
      assertEquals(1, reports.upserted.size());
      assertEquals(2, sources.inserted.size());
      assertEquals("https://example.com/market", sources.inserted.get(0).getUrl());
    } finally {
      server.stop(0);
    }
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

  private MarketNewsProvider testNewsProvider() {
    return ignored ->
        List.of(
            new MarketNewsArticle(
                1,
                "general",
                FIXED_CLOCK.instant().minusSeconds(3600),
                "시장 출처",
                "미국 금리와 주식시장 관련 사실 요약",
                "Reuters",
                "https://example.com/market"),
            new MarketNewsArticle(
                2,
                "forex",
                FIXED_CLOCK.instant().minusSeconds(7200),
                "복수 매체 출처",
                "달러와 원화 관련 사실 요약",
                "CNBC",
                "https://example.org/market"));
  }

  private MarketIndicatorSource source(MarketIndicatorType type, LocalDate dataAsOf) {
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
    private String prompt;

    @Override
    public MarketReportNarrative generate(String prompt) {
      this.prompt = prompt;
      return new MarketReportNarrative(
          "인용으로 확인한 테스트 시장 리포트",
          "확인된 사실을 한 줄로 정리한 테스트 요약",
          VALID_CONTENT,
          List.of(
              MarketReportSourceItem.builder()
                  .title("Finnhub 테스트 기사")
                  .url("https://www.investing.com/test")
                  .build(),
              MarketReportSourceItem.builder()
                  .title("복수 매체 테스트 기사")
                  .url("https://example.com/test")
                  .build()));
    }
  }

  private static class FailingNarrativeGenerator
      implements MarketReportNarrativeGenerator {
    private int calls;

    @Override
    public MarketReportNarrative generate(String prompt) {
      calls++;
      throw new AiCoachNarrativeGenerationException("테스트 실패");
    }
  }

  private static class RecordingReportMapper implements DailyMarketReportMapper {
    private final List<DailyMarketReportVo> upserted = new ArrayList<>();
    private final Map<LocalDate, DailyMarketReportVo> existing = new HashMap<>();

    @Override
    public int upsert(DailyMarketReportVo report) {
      DailyMarketReportVo current = existing.get(report.getReportDate());
      report.setReportId(current == null ? 1L : current.getReportId());
      existing.put(report.getReportDate(), report);
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
      return existing.containsKey(reportDate) ? 1 : 0;
    }

    @Override
    public int claimReportDate(
        LocalDate reportDate, LocalDateTime validFrom, LocalDateTime validUntil) {
      if (existing.containsKey(reportDate)) {
        return 0;
      }
      existing.put(
          reportDate,
          DailyMarketReportVo.builder()
              .reportId(1L)
              .reportDate(reportDate)
              .generationSource(MarketReportGenerationSource.FALLBACK)
              .promptVersion(MarketReportClaimService.PENDING_PROMPT_VERSION)
              .build());
      return 1;
    }

    @Override
    public DailyMarketReportVo findByReportDateForUpdate(LocalDate reportDate) {
      return existing.get(reportDate);
    }

    @Override
    public DailyMarketReportVo findByReportDate(LocalDate reportDate) {
      return existing.get(reportDate);
    }

    @Override
    public int markGenerationInProgress(long reportId) {
      for (DailyMarketReportVo report : existing.values()) {
        if (report.getReportId().equals(reportId)) {
          if (report.getGenerationSource() == MarketReportGenerationSource.GEMINI
              || MarketReportClaimService.IN_PROGRESS_PROMPT_VERSION.equals(
                  report.getPromptVersion())) {
            return 0;
          }
          report.setGenerationSource(MarketReportGenerationSource.FALLBACK);
          report.setReportStatus(MarketReportStatus.PARTIAL);
          report.setPromptVersion(MarketReportClaimService.IN_PROGRESS_PROMPT_VERSION);
          return 1;
        }
      }
      return 0;
    }

    @Override
    public int updateReportStatus(long reportId, String reportStatus) {
      for (DailyMarketReportVo report : existing.values()) {
        if (report.getReportId().equals(reportId)) {
          report.setReportStatus(MarketReportStatus.valueOf(reportStatus));
          return 1;
        }
      }
      return 0;
    }
  }

  private static class RecordingIndicatorMapper implements DailyMarketIndicatorMapper {
    private final List<DailyMarketIndicatorVo> inserted = new ArrayList<>();

    @Override
    public int insert(DailyMarketIndicatorVo indicator) {
      inserted.add(indicator);
      return 1;
    }

    @Override
    public int deleteByReportId(long reportId) {
      inserted.removeIf(indicator -> reportId == indicator.getReportId());
      return 1;
    }

    @Override
    public List<DailyMarketIndicatorVo> findByReportId(long reportId) {
      throw new UnsupportedOperationException();
    }
  }

  private static class RecordingSourceMapper implements DailyMarketReportSourceMapper {
    private final List<DailyMarketReportSourceVo> inserted = new ArrayList<>();

    @Override
    public int insert(DailyMarketReportSourceVo source) {
      inserted.add(source);
      return 1;
    }

    @Override
    public int deleteByReportId(long reportId) {
      inserted.removeIf(source -> reportId == source.getReportId());
      return 1;
    }

    @Override
    public List<DailyMarketReportSourceVo> findByReportId(long reportId) {
      return inserted.stream()
          .filter(source -> Long.valueOf(reportId).equals(source.getReportId()))
          .toList();
    }
  }

  private static void respond(
      com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private String structuredGeminiResponse() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "title", "오늘의 AI 시장 리포트",
                "summary", "Finnhub 뉴스 근거를 바탕으로 확인된 사실을 정리했습니다.",
                "content", VALID_CONTENT,
                "sourceIds", List.of("N1", "N2")));
    return objectMapper.writeValueAsString(
        Map.of(
            "candidates",
            List.of(
                Map.of(
                    "content",
                    Map.of(
                        "parts",
                        List.of(Map.of("text", payload)))))));
  }
}
