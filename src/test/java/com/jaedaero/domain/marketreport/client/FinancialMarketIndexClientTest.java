package com.jaedaero.domain.marketreport.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.marketreport.model.MarketIndex;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinancialMarketIndexClientTest {

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
  void getStockMarketIndexUsesGuideContractAndParsesJsonArray() {
    AtomicReference<String> method = new AtomicReference<>();
    AtomicReference<String> rawQuery = new AtomicReference<>();
    server.createContext(
        "/getStockMarketIndex",
        exchange -> {
          method.set(exchange.getRequestMethod());
          rawQuery.set(exchange.getRequestURI().getRawQuery());
          respond(
              exchange,
              200,
              """
              {
                "response": {
                  "header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE."},
                  "body": {
                    "numOfRows": 1,
                    "pageNo": 1,
                    "totalCount": 1,
                    "items": {"item": [{
                      "basDt": "20260807",
                      "idxNm": "코스피",
                      "clpr": "2650.12",
                      "vs": "12.30",
                      "fltRt": "0.47"
                    }]}
                  }
                }
              }
              """);
        });

    List<MarketIndex> items =
        newClient("test+service/key=").getStockMarketIndex("20260807", "코스피");

    String query = URLDecoder.decode(rawQuery.get(), StandardCharsets.UTF_8);
    assertEquals("GET", method.get());
    assertTrue(query.contains("serviceKey=test+service/key="));
    assertTrue(query.contains("resultType=json"));
    assertTrue(query.contains("pageNo=1"));
    assertTrue(query.contains("numOfRows=1"));
    assertTrue(query.contains("basDt=20260807"));
    assertTrue(query.contains("idxNm=코스피"));
    assertEquals("2026-08-07", items.get(0).baseDate().toString());
    assertEquals("코스피", items.get(0).indexName());
    assertEquals("2650.12", items.get(0).closingPrice().toString());
    assertEquals("12.30", items.get(0).change().toString());
    assertEquals("0.47", items.get(0).changeRate().toString());
  }

  @Test
  void getStockMarketIndexAcceptsSingleJsonItemObject() {
    server.createContext(
        "/getStockMarketIndex",
        exchange ->
            respond(
                exchange,
                200,
                """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
                "body":{"items":{"item":{"basDt":"20260807","idxNm":"코스닥",
                "clpr":"845.30","vs":"-3.10","fltRt":"-0.37"}}}}}
                """));

    List<MarketIndex> items =
        newClient("test-key").getStockMarketIndex("20260807", "코스닥");

    assertEquals(1, items.size());
    assertEquals("코스닥", items.get(0).indexName());
  }

  @Test
  void getStockMarketIndexReturnsEmptyWhenNoTradingDataExists() {
    server.createContext(
        "/getStockMarketIndex",
        exchange ->
            respond(
                exchange,
                200,
                """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
                "body":{"totalCount":0,"items":{}}}}
                """));

    assertTrue(
        newClient("test-key")
            .getStockMarketIndex("20260809", "코스피")
            .isEmpty());
  }

  @Test
  void getStockMarketIndexThrowsOnApiErrorCode() {
    server.createContext(
        "/getStockMarketIndex",
        exchange ->
            respond(
                exchange,
                200,
                """
                {"response":{"header":{"resultCode":"30","resultMsg":"SERVICE KEY IS NOT REGISTERED ERROR."}}}
                """));

    FinancialMarketIndexApiException exception =
        assertThrows(
            FinancialMarketIndexApiException.class,
            () -> newClient("invalid-key").getStockMarketIndex("20260807", "코스피"));

    assertTrue(exception.getMessage().contains("code=30"));
  }

  @Test
  void getStockMarketIndexThrowsOnHttpErrorAndMissingKey() {
    server.createContext(
        "/getStockMarketIndex", exchange -> respond(exchange, 500, "internal error"));

    assertThrows(
        FinancialMarketIndexApiException.class,
        () -> newClient("test-key").getStockMarketIndex("20260807", "코스피"));
    assertThrows(
        FinancialMarketIndexApiException.class,
        () -> newClient("").getStockMarketIndex("20260807", "코스피"));
  }

  private FinancialMarketIndexClient newClient(String serviceKey) {
    return new FinancialMarketIndexClient(
        baseUri, serviceKey, HttpClient.newHttpClient(), new ObjectMapper());
  }

  private static void respond(HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
    exchange.sendResponseHeaders(statusCode, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }
}
