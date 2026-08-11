package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FredTreasuryYieldClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String PATH = "/fred/series/observations";
  private static final String SERIES_ID = "DGS10";

  private final URI baseUri;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public FredTreasuryYieldClient(
      @Value("${fred.api.base-url:https://api.stlouisfed.org}") String baseUrl,
      @Value("${fred.api.key:}") String apiKey) {
    this(
        URI.create(baseUrl),
        apiKey,
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
        new ObjectMapper());
  }

  FredTreasuryYieldClient(
      URI baseUri, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.baseUri = baseUri;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public FredObservationsResponse getRecentObservations(int limit) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new FredApiException(
          "FRED API 인증키가 설정되지 않았습니다. application-local.properties에 fred.api.key를 설정하세요.");
    }
    try {
      URI uri =
          baseUri.resolve(
              PATH
                  + "?series_id="
                  + SERIES_ID
                  + "&api_key="
                  + apiKey
                  + "&file_type=json&sort_order=desc&limit="
                  + limit);
      HttpRequest request =
          HttpRequest.newBuilder(uri)
              .timeout(REQUEST_TIMEOUT)
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(
              request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new FredApiException(
            "FRED 미국채10년물 조회에 실패했습니다. status=" + response.statusCode());
      }
      return objectMapper.readValue(response.body(), FredObservationsResponse.class);
    } catch (FredApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new FredApiException("FRED 미국채10년물 조회 요청이 중단되었습니다.", exception);
    } catch (Exception exception) {
      throw new FredApiException("FRED 미국채10년물 조회에 실패했습니다.", exception);
    }
  }
}
