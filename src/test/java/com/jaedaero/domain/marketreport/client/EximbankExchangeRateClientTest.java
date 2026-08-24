package com.jaedaero.domain.marketreport.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EximbankExchangeRateClientTest {

  private HttpServer server;
  private URI baseUri;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();
    baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void getExchangeRatesSendsAuthKeyAndParsesUsdRate() {
    AtomicReference<String> query = new AtomicReference<>();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          query.set(exchange.getRequestURI().getQuery());
          respond(
              exchange,
              """
              [{"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1,300.00",
                "tts":"1,340.00","deal_bas_r":"1,320.50","bkpr":"1,320"},
               {"result":1,"cur_unit":"JPY(100)","cur_nm":"일본 옌","ttb":"880.00",
                "tts":"900.00","deal_bas_r":"890.10","bkpr":"890"}]
              """);
        });

    List<EximbankExchangeRateItem> rates =
        newClient().getExchangeRates("20260806");

    assertTrue(query.get().contains("authkey=test-eximbank-key"));
    assertTrue(query.get().contains("searchdate=20260806"));
    assertEquals("1,320.50", rates.get(0).dealBasR());
  }

  @Test
  void getExchangeRatesReturnsEmptyListOnNonBusinessDay() {
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> respond(exchange, "[]"));

    assertTrue(newClient().getExchangeRates("20260809").isEmpty());
  }

  @Test
  void getExchangeRatesThrowsOnServerError() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          requestCount.incrementAndGet();
          respond(exchange, 500, "internal error");
        });

    EximbankApiException exception = assertThrows(
        EximbankApiException.class, () -> newClient().getExchangeRates("20260806"));

    assertEquals(3, requestCount.get());
    assertTrue(exception.getMessage().contains("status=500"));
    assertTrue(exception.getMessage().contains("attempt=3/3"));
  }

  @Test
  void getExchangeRatesRetriesTemporaryServerErrorAndRecovers() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          if (requestCount.incrementAndGet() == 1) {
            respond(exchange, 500, "temporary error");
            return;
          }
          respond(
              exchange,
              "[{\"result\":1,\"cur_unit\":\"USD\",\"deal_bas_r\":\"1,320.50\"}]");
        });

    List<EximbankExchangeRateItem> rates =
        newClient().getExchangeRates("20260806");

    assertEquals(2, requestCount.get());
    assertEquals("1,320.50", rates.get(0).dealBasR());
  }

  @Test
  void getExchangeRatesRetriesRateLimitAndRecovers() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          if (requestCount.incrementAndGet() == 1) {
            respond(exchange, 429, "rate limited");
            return;
          }
          respond(exchange, "[]");
        });

    assertTrue(newClient().getExchangeRates("20260806").isEmpty());
    assertEquals(2, requestCount.get());
  }

  @Test
  void getExchangeRatesDoesNotRetryNonRetryableClientError() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          requestCount.incrementAndGet();
          respond(exchange, 400, "bad request");
        });

    EximbankApiException exception =
        assertThrows(
            EximbankApiException.class,
            () -> newClient().getExchangeRates("20260806"));

    assertEquals(1, requestCount.get());
    assertTrue(exception.getMessage().contains("status=400"));
  }

  @Test
  void getExchangeRatesThrowsWhenApiReturnsFailureResultCode() {
    AtomicInteger requestCount = new AtomicInteger();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          requestCount.incrementAndGet();
          respond(exchange, "[{\"result\":4}]");
        });

    EximbankApiException exception =
        assertThrows(
            EximbankApiException.class,
            () -> newClient().getExchangeRates("20260806"));

    assertEquals(1, requestCount.get());
    assertTrue(exception.getMessage().contains("resultCodes=[4]"));
  }

  @Test
  void getExchangeRatesThrowsOnMalformedJson() {
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> respond(exchange, 200, "not-json"));

    assertThrows(
        EximbankApiException.class, () -> newClient().getExchangeRates("20260806"));
  }

  @Test
  void getExchangeRatesThrowsWhenApiKeyMissing() {
    EximbankExchangeRateClient client =
        new EximbankExchangeRateClient(
            baseUri, "", HttpClient.newHttpClient(), new ObjectMapper());

    assertThrows(EximbankApiException.class, () -> client.getExchangeRates("20260806"));
  }

  private EximbankExchangeRateClient newClient() {
    return new EximbankExchangeRateClient(
        baseUri,
        "test-eximbank-key",
        HttpClient.newHttpClient(),
        new ObjectMapper(),
        3,
        Duration.ZERO,
        duration -> {});
  }

  private static void respond(HttpExchange exchange, String body)
      throws IOException {
    respond(exchange, 200, body);
  }

  private static void respond(HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(statusCode, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }
}
