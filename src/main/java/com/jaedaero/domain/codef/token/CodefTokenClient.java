package com.jaedaero.domain.codef.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Server-side client for CODEF OAuth token issuance.
 *
 * <p>Client credentials must be supplied through application-local.properties or deployment
 * environment variables. They must never be sent to the frontend or written to logs.
 */
@Component
public class CodefTokenClient {

    private static final String TOKEN_REQUEST_BODY = "grant_type=client_credentials&scope=read";

    private final URI tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public CodefTokenClient(
            @Value("${codef.oauth.token-url}") String tokenUrl,
            @Value("${codef.client-id}") String clientId,
            @Value("${codef.client-secret}") String clientSecret,
            @Value("${codef.oauth.timeout-seconds:30}") long timeoutSeconds) {
        this(
                URI.create(tokenUrl),
                clientId,
                clientSecret,
                Duration.ofSeconds(timeoutSeconds),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build(),
                new ObjectMapper());
    }

    CodefTokenClient(
            URI tokenUri,
            String clientId,
            String clientSecret,
            Duration timeout,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.timeout = timeout;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public CodefTokenResponse publishToken() {
        HttpRequest request = HttpRequest.newBuilder(tokenUri)
                .timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", basicAuthorizationValue())
                .POST(HttpRequest.BodyPublishers.ofString(TOKEN_REQUEST_BODY, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CodefTokenException("CODEF token issuance failed.", response.statusCode());
            }

            return objectMapper.readValue(response.body(), CodefTokenResponse.class);
        } catch (JsonProcessingException exception) {
            throw new CodefTokenException("CODEF token response could not be parsed.", 502, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CodefTokenException("CODEF token request was interrupted.", 503, exception);
        } catch (CodefTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CodefTokenException("CODEF token request failed.", 503, exception);
        }
    }

    private String basicAuthorizationValue() {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }
}
