package com.jaedaero.domain.marketreport.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.aianalysis.llm.OpenAiModel;
import com.jaedaero.domain.marketreport.dto.MarketCondition;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class OpenAiMarketReportNarrativeGeneratorTest {

  private HttpServer server;
  private URI endpoint;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();
    endpoint =
        URI.create(
            "http://localhost:"
                + server.getAddress().getPort()
                + "/v1/chat/completions");
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void generateParsesMarketConditionAndContent() {
    server.createContext(
        "/v1/chat/completions",
        exchange -> {
          String body =
              """
              {"choices":[{"message":{"content":"{\\"marketCondition\\":\\"BULL\\",\\"content\\":\\"코스피가 상승 마감했습니다.\\"}"}}]}
              """;
          byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    var generator =
        new OpenAiMarketReportNarrativeGenerator(
            new RestTemplate(), "test-llm-key", endpoint.toString());

    MarketReportNarrative narrative =
        generator.generate(OpenAiModel.GPT_5_NANO, "오늘의 지표 요약");

    assertEquals(MarketCondition.BULL, narrative.marketCondition());
    assertEquals("코스피가 상승 마감했습니다.", narrative.content());
  }

  @Test
  void generateThrowsOnNonSuccessHttpResponse() {
    server.createContext(
        "/v1/chat/completions",
        exchange -> respond(exchange, 500, "{\"error\":\"internal\"}"));
    var generator =
        new OpenAiMarketReportNarrativeGenerator(
            new RestTemplate(), "test-llm-key", endpoint.toString());

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator.generate(OpenAiModel.GPT_5_NANO, "오늘의 지표 요약"));
  }

  @Test
  void generateThrowsWhenChoicesMissing() {
    server.createContext(
        "/v1/chat/completions", exchange -> respond(exchange, 200, "{\"choices\":[]}"));
    var generator =
        new OpenAiMarketReportNarrativeGenerator(
            new RestTemplate(), "test-llm-key", endpoint.toString());

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator.generate(OpenAiModel.GPT_5_NANO, "오늘의 지표 요약"));
  }

  @Test
  void generateThrowsWhenMarketConditionInvalid() {
    server.createContext(
        "/v1/chat/completions",
        exchange ->
            respond(
                exchange,
                200,
                "{\"choices\":[{\"message\":{\"content\":"
                    + "\"{\\\"marketCondition\\\":\\\"SIDEWAYS\\\",\\\"content\\\":\\\"요약\\\"}\"}}]}"));
    var generator =
        new OpenAiMarketReportNarrativeGenerator(
            new RestTemplate(), "test-llm-key", endpoint.toString());

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator.generate(OpenAiModel.GPT_5_NANO, "오늘의 지표 요약"));
  }

  @Test
  void generateThrowsWhenContentEmpty() {
    server.createContext(
        "/v1/chat/completions",
        exchange ->
            respond(
                exchange,
                200,
                "{\"choices\":[{\"message\":{\"content\":"
                    + "\"{\\\"marketCondition\\\":\\\"BULL\\\",\\\"content\\\":\\\"\\\"}\"}}]}"));
    var generator =
        new OpenAiMarketReportNarrativeGenerator(
            new RestTemplate(), "test-llm-key", endpoint.toString());

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator.generate(OpenAiModel.GPT_5_NANO, "오늘의 지표 요약"));
  }

  private static void respond(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  @Test
  void generateThrowsWhenApiKeyMissing() {
    var generator =
        new OpenAiMarketReportNarrativeGenerator(
            new RestTemplate(), "", endpoint.toString());

    assertThrows(
        AiCoachNarrativeGenerationException.class,
        () -> generator.generate(OpenAiModel.GPT_5_NANO, "프롬프트"));
  }
}
