package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.marketreport.client.EximbankExchangeRateClient;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsdKrwIndicatorSourceTest {

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
  void fetchLooksBackAndStripsComma() {
    LocalDate businessDate = LocalDate.of(2026, 8, 9);
    LocalDate previousBusinessDay = businessDate.minusDays(2);
    List<String> requestedQueries = new ArrayList<>();
    server.createContext(
        "/site/program/financial/exchangeJSON",
        exchange -> {
          String query = exchange.getRequestURI().getQuery();
          requestedQueries.add(query);
          String body =
              query.contains(
                      "searchdate="
                          + previousBusinessDay.toString().replace("-", ""))
                  ? "[{\"result\":1,\"cur_unit\":\"USD\",\"cur_nm\":\"미국 달러\",\"deal_bas_r\":\"1,320.50\"}]"
                  : "[]";
          byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    EximbankExchangeRateClient client =
        new EximbankExchangeRateClient(baseUri.toString(), "test-key");

    MarketIndicatorObservation observation =
        new UsdKrwIndicatorSource(client).fetch(businessDate).orElseThrow();

    assertEquals(previousBusinessDay, observation.dataAsOf());
    assertEquals(new BigDecimal("1320.50"), observation.observedValue());
    assertTrue(
        requestedQueries.get(0).contains(
            "searchdate=" + businessDate.minusDays(1).toString().replace("-", "")));
    assertTrue(
        requestedQueries.stream()
            .noneMatch(
                query ->
                    query.contains(
                        "searchdate=" + businessDate.toString().replace("-", ""))));
  }

}
