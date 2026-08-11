package com.jaedaero.domain.marketreport.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.marketreport.service.KospiIndicatorSource;
import com.jaedaero.domain.marketreport.service.MarketIndicatorObservation;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KospiIndicatorSourceTest {

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
  void fetchStartsFromPreviousDayAndLooksBackAcrossMarketClosedDays() {
    LocalDate requestedDate = LocalDate.of(2026, 8, 9);
    LocalDate actualTradingDate = LocalDate.of(2026, 8, 7);
    List<String> requestedBaseDates = new ArrayList<>();
    server.createContext(
        "/getStockMarketIndex",
        exchange -> {
          String rawQuery = exchange.getRequestURI().getRawQuery();
          String basDt = queryValue(rawQuery, "basDt");
          requestedBaseDates.add(basDt);
          String body =
              "20260807".equals(basDt)
                  ? "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL SERVICE.\"},"
                      + "\"body\":{\"items\":{\"item\":[{\"basDt\":\"20260807\",\"idxNm\":\"코스피\","
                      + "\"clpr\":\"2650.12\",\"vs\":\"12.30\",\"fltRt\":\"0.47\"}]}}}}"
                  : "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL SERVICE.\"},"
                      + "\"body\":{\"totalCount\":0,\"items\":{}}}}";
          byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    FinancialMarketIndexClient client =
        new FinancialMarketIndexClient(
            baseUri, "test-key", HttpClient.newHttpClient(), new ObjectMapper());

    MarketIndicatorObservation observation =
        new KospiIndicatorSource(client).fetch(requestedDate).orElseThrow();

    assertEquals(actualTradingDate, observation.dataAsOf());
    assertEquals(List.of("20260808", "20260807"), requestedBaseDates);
  }

  private String queryValue(String rawQuery, String name) {
    for (String pair : rawQuery.split("&")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2 && name.equals(parts[0])) {
        return parts[1];
      }
    }
    throw new IllegalArgumentException("query parameter not found: " + name);
  }
}
