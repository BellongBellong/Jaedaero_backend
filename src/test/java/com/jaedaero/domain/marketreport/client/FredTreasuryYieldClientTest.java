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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FredTreasuryYieldClientTest {

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
  void getRecentObservationsSendsApiKeyAndSeriesId() {
    AtomicReference<String> query = new AtomicReference<>();
    AtomicReference<String> path = new AtomicReference<>();
    AtomicReference<String> method = new AtomicReference<>();
    server.createContext(
        "/fred/series/observations",
        exchange -> {
          method.set(exchange.getRequestMethod());
          path.set(exchange.getRequestURI().getPath());
          query.set(exchange.getRequestURI().getQuery());
          respond(
              exchange,
              """
              {"realtime_start":"2026-08-11","realtime_end":"2026-08-11","count":3,
               "observations":[
                 {"realtime_start":"2026-08-06","realtime_end":"2026-08-06","date":"2026-08-06","value":"4.25"},
                 {"realtime_start":"2026-08-05","realtime_end":"2026-08-05","date":"2026-08-05","value":"."},
                 {"realtime_start":"2026-08-04","realtime_end":"2026-08-04","date":"2026-08-04","value":"4.20"}]}
              """);
        });

    FredObservationsResponse response =
        newClient().getRecentObservations(10);

    assertTrue(query.get().contains("series_id=DGS10"));
    assertTrue(query.get().contains("api_key=test-fred-key"));
    assertTrue(query.get().contains("file_type=json"));
    assertTrue(query.get().contains("sort_order=desc"));
    assertTrue(query.get().contains("limit=10"));
    assertEquals("GET", method.get());
    assertEquals("/fred/series/observations", path.get());
    assertEquals("4.25", response.observations().get(0).value());
  }

  @Test
  void getRecentObservationsThrowsOnServerError() {
    server.createContext(
        "/fred/series/observations",
        exchange -> respond(exchange, 500, "internal error"));

    assertThrows(FredApiException.class, () -> newClient().getRecentObservations(10));
  }

  @Test
  void getRecentObservationsThrowsOnMalformedJson() {
    server.createContext(
        "/fred/series/observations", exchange -> respond(exchange, 200, "not-json"));

    assertThrows(FredApiException.class, () -> newClient().getRecentObservations(10));
  }

  @Test
  void getRecentObservationsThrowsWhenApiKeyMissing() {
    FredTreasuryYieldClient client =
        new FredTreasuryYieldClient(
            baseUri, "", HttpClient.newHttpClient(), new ObjectMapper());

    assertThrows(FredApiException.class, () -> client.getRecentObservations(10));
  }

  private FredTreasuryYieldClient newClient() {
    return new FredTreasuryYieldClient(
        baseUri,
        "test-fred-key",
        HttpClient.newHttpClient(),
        new ObjectMapper());
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
