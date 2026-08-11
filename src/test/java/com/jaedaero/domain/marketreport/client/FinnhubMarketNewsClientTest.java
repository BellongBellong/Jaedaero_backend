package com.jaedaero.domain.marketreport.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinnhubMarketNewsClientTest {

  private HttpServer server;
  private URI baseUri;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();
    baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void requestsCategoryAndHeaderTokenWithoutExposingTokenInUri() {
    AtomicReference<String> query = new AtomicReference<>();
    AtomicReference<String> tokenHeader = new AtomicReference<>();
    server.createContext(
        "/api/v1/news",
        exchange -> {
          query.set(exchange.getRequestURI().getRawQuery());
          tokenHeader.set(exchange.getRequestHeaders().getFirst("X-Finnhub-Token"));
          byte[] body =
              """
              [{
                "category":"general",
                "datetime":1786400000,
                "headline":"Fed decision moves global markets",
                "id":101,
                "image":"https://example.com/image.jpg",
                "related":"SPY",
                "source":"Reuters",
                "summary":"Treasury yields and the dollar moved after the decision.",
                "url":"https://example.com/article"
              }]
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });

    FinnhubMarketNewsClient client =
        new FinnhubMarketNewsClient(
            baseUri, "test key", HttpClient.newHttpClient(), new ObjectMapper());

    var result = client.getMarketNews("general");

    assertEquals(1, result.size());
    assertEquals(101, result.get(0).id());
    assertEquals("Reuters", result.get(0).source());
    assertTrue(query.get().contains("category=general"));
    assertEquals("test key", tokenHeader.get());
    assertFalse(query.get().contains("token"));
    assertFalse(query.get().contains("test%20key"));
    assertFalse(query.get().contains("test+key"));
  }

  @Test
  void rejectsMissingKeyAndUnsupportedCategory() {
    FinnhubMarketNewsClient missingKeyClient =
        new FinnhubMarketNewsClient(
            baseUri, "", HttpClient.newHttpClient(), new ObjectMapper());
    assertThrows(
        FinnhubApiException.class, () -> missingKeyClient.getMarketNews("general"));

    FinnhubMarketNewsClient client =
        new FinnhubMarketNewsClient(
            baseUri, "test-key", HttpClient.newHttpClient(), new ObjectMapper());
    assertThrows(IllegalArgumentException.class, () -> client.getMarketNews("stocks"));
  }
}
