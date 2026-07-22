package com.jaedaero.codef.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.codef.token.CodefAccessTokenProvider;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Common CODEF resource API client following the official Java sample request format. */
@Component
public class CodefApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final URI apiBaseUri;
    private final CodefAccessTokenProvider accessTokenProvider;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public CodefApiClient(
            @Value("${codef.api.base-url}") String apiBaseUrl,
            CodefAccessTokenProvider accessTokenProvider) {
        this(
                URI.create(apiBaseUrl),
                accessTokenProvider,
                HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
                new ObjectMapper());
    }

    CodefApiClient(
            URI apiBaseUri,
            CodefAccessTokenProvider accessTokenProvider,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.apiBaseUri = apiBaseUri;
        this.accessTokenProvider = accessTokenProvider;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode post(String path, Object requestBody) {
        String encodedBody = encodeRequestBody(requestBody);
        JsonNode response = execute(path, encodedBody, accessTokenProvider.getAccessToken());

        if ("invalid_token".equals(response.path("error").asText())) {
            accessTokenProvider.invalidate();
            response = execute(path, encodedBody, accessTokenProvider.getAccessToken());
        }

        return response;
    }

    private String encodeRequestBody(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            return URLEncoder.encode(json, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new CodefApiException("CODEF request could not be serialized.", 500, exception);
        }
    }

    private JsonNode execute(String path, String encodedBody, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(apiBaseUri.resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(encodedBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String decodedBody = URLDecoder.decode(response.body(), StandardCharsets.UTF_8);
            JsonNode responseBody = objectMapper.readTree(decodedBody);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CodefApiException("CODEF resource API request failed.", response.statusCode());
            }

            return responseBody;
        } catch (CodefApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CodefApiException("CODEF resource API request was interrupted.", 503, exception);
        } catch (Exception exception) {
            throw new CodefApiException("CODEF resource API request failed.", 503, exception);
        }
    }
}
