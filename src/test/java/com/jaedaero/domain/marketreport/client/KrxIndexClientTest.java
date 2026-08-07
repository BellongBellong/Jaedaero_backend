package com.jaedaero.domain.marketreport.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class KrxIndexClientTest {

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
  void getKospiDailyTradingSendsAuthHeaderAndParsesIndex() {
    AtomicReference<String> authKey = new AtomicReference<>();
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/svc/apis/idx/kospi_dd_trd",
        exchange -> {
          authKey.set(exchange.getRequestHeaders().getFirst("AUTH_KEY"));
          requestBody.set(
              new String(
                  exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          respond(
              exchange,
              200,
              """
              {"OutBlock_1":[{"BAS_DD":"20260806","IDX_NM":"코스피","CLSPRC_IDX":"2650.12","CMPPREVDD_IDX":"12.30","FLUC_RT_IDX":"0.47"}]}
              """);
        });

    KrxIndexDailyTradingResponse response =
        newClient().getKospiDailyTrading("20260806");

    assertEquals("test-krx-key", authKey.get());
    assertTrue(requestBody.get().contains("20260806"));
    assertEquals("2650.12", response.outBlock1().get(0).clsprcIdx());
  }

  @Test
  void getKosdaqDailyTradingReturnsEmptyOutBlockWhenMarketClosed() {
    server.createContext(
        "/svc/apis/idx/kosdaq_dd_trd",
        exchange -> respond(exchange, 200, "{\"OutBlock_1\":[]}"));

    KrxIndexDailyTradingResponse response =
        newClient().getKosdaqDailyTrading("20260809");

    assertTrue(response.outBlock1().isEmpty());
  }

  private KrxIndexClient newClient() {
    return new KrxIndexClient(
        baseUri,
        "test-krx-key",
        HttpClient.newHttpClient(),
        new ObjectMapper());
  }

  private static void respond(
      HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(statusCode, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }
}
