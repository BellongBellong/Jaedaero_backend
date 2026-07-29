package com.jaedaero.domain.investment.etf;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class KrxEtfClientTest {

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
    void getDailyTrading_sendsKrxAuthHeaderAndBaseDate() {
        AtomicReference<String> authKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/svc/apis/etp/etf_bydd_trd", exchange -> {
            authKey.set(exchange.getRequestHeaders().getFirst("AUTH_KEY"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"OutBlock_1":[{"BAS_DD":"20260728","ISU_CD":"069500","ISU_NM":"KODEX 200","TDD_CLSPRC":"50000"}]}
                    """);
        });

        EtfDailyTradingResponse response = newClient().getDailyTrading("20260728");

        assertEquals("test-krx-key", authKey.get());
        assertEquals("{\"basDd\":\"20260728\"}", requestBody.get());
        assertEquals(1, response.outBlock1().size());
        assertEquals("069500", response.outBlock1().get(0).isuCd());
        assertEquals("KODEX 200", response.outBlock1().get(0).isuNm());
    }

    private KrxEtfClient newClient() {
        return new KrxEtfClient(baseUri, "test-krx-key", HttpClient.newHttpClient(), new ObjectMapper());
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }
}
