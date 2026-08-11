package com.jaedaero.domain.marketreport.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class GeminiMarketReportNarrativeGeneratorTest {

  private static final String VALID_CONTENT =
      "오늘 시장의 주요 지표와 확인된 뉴스 흐름을 중심으로 사실과 불확실성을 구분해 설명합니다. "
          .repeat(12)
          .trim();

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;
  private String endpoint;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();
    endpoint =
        "http://localhost:"
            + server.getAddress().getPort()
            + "/v1beta/models/gemini-3.6-flash:generateContent";
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void generateUsesSchemaWithoutSearchAndMapsUsedFinnhubSourceIds() {
    AtomicInteger calls = new AtomicInteger();
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange -> {
          calls.incrementAndGet();
          assertEquals("POST", exchange.getRequestMethod());
          assertEquals("test-gemini-key", exchange.getRequestHeaders().getFirst("x-goog-api-key"));
          assertFalse(exchange.getRequestHeaders().containsKey("Authorization"));

          JsonNode request =
              objectMapper.readTree(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          assertEquals(
              "Finnhub 근거가 포함된 프롬프트",
              request.path("contents").path(0).path("parts").path(0).path("text").asText());
          assertFalse(request.has("tools"));
          assertEquals(
              "application/json",
              request.path("generationConfig").path("responseMimeType").asText());
          JsonNode schema = request.path("generationConfig").path("responseSchema");
          assertEquals("OBJECT", schema.path("type").asText());
          assertEquals(
              "ARRAY", schema.path("properties").path("sourceIds").path("type").asText());
          assertTrue(schema.path("required").toString().contains("sourceIds"));

          respond(
              exchange,
              200,
              generateContentResponse(
                  structuredPayload(
                      "오늘의 AI 시장 리포트",
                      "오늘 시장의 핵심 흐름을\n한 줄로 정리했습니다.",
                      VALID_CONTENT,
                      List.of("N2", "N1", "N2"))));
        });

    MarketReportNarrative narrative =
        generator("test-gemini-key")
            .generate("Finnhub 근거가 포함된 프롬프트", availableSources());

    assertEquals(1, calls.get());
    assertEquals("오늘의 AI 시장 리포트", narrative.title());
    assertEquals("오늘 시장의 핵심 흐름을 한 줄로 정리했습니다.", narrative.summary());
    assertEquals(VALID_CONTENT, narrative.content());
    assertEquals(2, narrative.sources().size());
    assertEquals("두 번째 Finnhub 기사", narrative.sources().get(0).getTitle());
    assertEquals("https://example.com/first", narrative.sources().get(1).getUrl());
  }

  @Test
  void generateRejectsFewerThanTwoUsedSources() throws Exception {
    respondWith(structuredPayload("제목", "요약", VALID_CONTENT, List.of("N1")));

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator("test-gemini-key").generate("출처 검증", availableSources()));
  }

  @Test
  void generateRejectsUnknownSourceId() throws Exception {
    respondWith(structuredPayload("제목", "요약", VALID_CONTENT, List.of("N1", "N9")));

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator("test-gemini-key").generate("출처 검증", availableSources()));
  }

  @Test
  void generateRejectsStructuredContentShorterThan300KoreanCharacters() throws Exception {
    respondWith(structuredPayload("제목", "요약", "짧은 본문입니다.", List.of("N1", "N2")));

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator("test-gemini-key").generate("본문 길이 검증", availableSources()));
  }

  @Test
  void generateRejectsTitleOrSummaryBeyondDatabaseLength() throws Exception {
    respondWith(
        structuredPayload("제목", "가".repeat(501), VALID_CONTENT, List.of("N1", "N2")));

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator("test-gemini-key").generate("요약 길이 검증", availableSources()));
  }

  @Test
  void generateRejectsNonSuccessHttpResponseWithoutLeakingApiKey() {
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange ->
            respond(
                exchange,
                429,
                """
                {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"quota exceeded for test-gemini-key"}}
                """));

    AiCoachNarrativeGenerationException exception =
        assertThrows(
            AiCoachNarrativeGenerationException.class,
            () -> generator("test-gemini-key").generate("프롬프트", availableSources()));
    assertTrue(exception.getMessage().contains("HTTP 429"));
    assertTrue(exception.getMessage().contains("RESOURCE_EXHAUSTED"));
    assertFalse(exception.getMessage().contains("test-gemini-key"));
  }

  @Test
  void generateRetriesTemporary503AndThenSucceeds() {
    AtomicInteger calls = new AtomicInteger();
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange -> {
          if (calls.incrementAndGet() == 1) {
            respond(
                exchange,
                503,
                """
                {"error":{"code":503,"status":"UNAVAILABLE","message":"temporary overload"}}
                """);
            return;
          }
          respond(
              exchange,
              200,
              generateContentResponse(
                  structuredPayload("제목", "요약", VALID_CONTENT, List.of("N1", "N2"))));
        });

    MarketReportNarrative narrative =
        new GeminiMarketReportNarrativeGenerator(
                new RestTemplate(), objectMapper, "test-gemini-key", endpoint, 3, 0L)
            .generate("프롬프트", availableSources());

    assertEquals(2, calls.get());
    assertEquals("제목", narrative.title());
  }

  @Test
  void generateDoesNotRetryNonTemporary400() {
    AtomicInteger calls = new AtomicInteger();
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange -> {
          calls.incrementAndGet();
          respond(
              exchange,
              400,
              """
              {"error":{"code":400,"status":"INVALID_ARGUMENT","message":"invalid request"}}
              """);
        });

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () ->
            new GeminiMarketReportNarrativeGenerator(
                    new RestTemplate(), objectMapper, "test-gemini-key", endpoint, 3, 0L)
                .generate("프롬프트", availableSources()));

    assertEquals(1, calls.get());
  }

  @Test
  void generateRejectsMissingApiKeyWithoutHttpRequest() {
    AtomicInteger calls = new AtomicInteger();
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange -> {
          calls.incrementAndGet();
          respond(exchange, 200, "{}");
        });

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator("").generate("프롬프트", availableSources()));
    assertEquals(0, calls.get());
  }

  private void respondWith(String payload) {
    server.createContext(
        "/v1beta/models/gemini-3.6-flash:generateContent",
        exchange -> respond(exchange, 200, generateContentResponse(payload)));
  }

  private GeminiMarketReportNarrativeGenerator generator(String apiKey) {
    return new GeminiMarketReportNarrativeGenerator(
        new RestTemplate(), objectMapper, apiKey, endpoint);
  }

  private List<MarketReportSourceItem> availableSources() {
    return List.of(
        MarketReportSourceItem.builder()
            .title("첫 번째 Finnhub 기사")
            .url("https://example.com/first")
            .build(),
        MarketReportSourceItem.builder()
            .title("두 번째 Finnhub 기사")
            .url("https://example.org/second")
            .build());
  }

  private static void respond(
      com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private String structuredPayload(
      String title, String summary, String content, List<String> sourceIds) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "title", title,
            "summary", summary,
            "content", content,
            "sourceIds", sourceIds));
  }

  private String generateContentResponse(String payload) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "candidates",
            List.of(
                Map.of(
                    "content",
                    Map.of("role", "model", "parts", List.of(Map.of("text", payload)))))));
  }
}
