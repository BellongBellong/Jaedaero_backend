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
  void fetchUsesResponseBasDdAsDataAsOfRatherThanRequestedDate() {
    LocalDate requestedDate = LocalDate.of(2026, 8, 9);
    LocalDate actualTradingDate = LocalDate.of(2026, 8, 7);
    server.createContext(
        "/svc/apis/idx/kospi_dd_trd",
        exchange -> {
          String body =
              "{\"OutBlock_1\":[{\"BAS_DD\":\"20260807\",\"IDX_NM\":\"코스피\","
                  + "\"CLSPRC_IDX\":\"2650.12\",\"CMPPREVDD_IDX\":\"12.30\",\"FLUC_RT_IDX\":\"0.47\"}]}";
          byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    KrxIndexClient client =
        new KrxIndexClient(baseUri, "test-key", HttpClient.newHttpClient(), new ObjectMapper());

    MarketIndicatorObservation observation =
        new KospiIndicatorSource(client).fetch(requestedDate).orElseThrow();

    assertEquals(actualTradingDate, observation.dataAsOf());
  }
}
