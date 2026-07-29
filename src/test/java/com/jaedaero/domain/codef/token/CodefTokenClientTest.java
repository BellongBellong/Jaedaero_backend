package com.jaedaero.domain.codef.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodefTokenClientTest {

    private HttpServer server;
    private URI tokenUri;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        tokenUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/oauth/token");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void publishToken_sendsClientCredentialsAndParsesTokenResponse() {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server.createContext("/oauth/token", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"access_token\":\"test-token\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"read\"}");
        });

        CodefTokenClient client = newClient();

        CodefTokenResponse response = client.publishToken();

        assertEquals("test-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals("read", response.getScope());
        assertEquals("Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=", authorization.get());
        assertEquals("application/x-www-form-urlencoded", contentType.get());
        assertEquals("grant_type=client_credentials&scope=read", requestBody.get());
    }

    @Test
    void publishToken_throwsExceptionWhenCodefReturnsError() {
        server.createContext("/oauth/token", exchange -> respond(exchange, 401, "unauthorized"));

        CodefTokenException exception = assertThrows(CodefTokenException.class, () -> newClient().publishToken());

        assertEquals(401, exception.getStatusCode());
    }

    private CodefTokenClient newClient() {
        return new CodefTokenClient(
                tokenUri,
                "client-id",
                "client-secret",
                Duration.ofSeconds(3),
                HttpClient.newHttpClient(),
                new ObjectMapper());
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }
}
