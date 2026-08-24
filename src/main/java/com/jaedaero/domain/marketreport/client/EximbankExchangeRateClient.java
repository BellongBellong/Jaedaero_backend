package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EximbankExchangeRateClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration INITIAL_RETRY_DELAY = Duration.ofMillis(500);
  private static final int MAX_ATTEMPTS = 3;
  private static final String PATH = "/site/program/financial/exchangeJSON";

  private final URI baseUri;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final int maxAttempts;
  private final Duration initialRetryDelay;
  private final RetrySleeper retrySleeper;

  @Autowired
  public EximbankExchangeRateClient(
      @Value("${eximbank.api.base-url:https://oapi.koreaexim.go.kr}") String baseUrl,
      @Value("${eximbank.api.key:}") String apiKey) {
    this(
        URI.create(baseUrl),
        apiKey,
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
        new ObjectMapper());
  }

  EximbankExchangeRateClient(
      URI baseUri, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this(
        baseUri,
        apiKey,
        httpClient,
        objectMapper,
        MAX_ATTEMPTS,
        INITIAL_RETRY_DELAY,
        duration -> Thread.sleep(duration.toMillis()));
  }

  EximbankExchangeRateClient(
      URI baseUri,
      String apiKey,
      HttpClient httpClient,
      ObjectMapper objectMapper,
      int maxAttempts,
      Duration initialRetryDelay,
      RetrySleeper retrySleeper) {
    this.baseUri = baseUri;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.maxAttempts = maxAttempts;
    this.initialRetryDelay = initialRetryDelay;
    this.retrySleeper = retrySleeper;
  }

  public List<EximbankExchangeRateItem> getExchangeRates(String searchDate) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new EximbankApiException(
          "한국수출입은행 API 인증키가 설정되지 않았습니다. application-local.properties에 eximbank.api.key를 설정하세요.");
    }
    URI uri = buildUri(searchDate);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();

    EximbankApiException lastFailure = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        HttpResponse<String> response =
            httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          EximbankApiException failure =
              new EximbankApiException(
                  "한국수출입은행 환율 조회 HTTP 오류입니다. searchDate="
                      + searchDate
                      + ", status="
                      + response.statusCode()
                      + ", attempt="
                      + attempt
                      + "/"
                      + maxAttempts);
          if (!isRetryableStatus(response.statusCode()) || attempt == maxAttempts) {
            throw failure;
          }
          lastFailure = failure;
        } else {
          List<EximbankExchangeRateItem> rates = parseRates(response.body(), searchDate, attempt);
          validateResultCodes(rates, searchDate);
          return rates;
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new EximbankApiException(
            "한국수출입은행 환율 조회 요청이 중단되었습니다. searchDate=" + searchDate,
            exception);
      } catch (IOException exception) {
        lastFailure =
            new EximbankApiException(
                "한국수출입은행 환율 조회 통신 오류입니다. searchDate="
                    + searchDate
                    + ", attempt="
                    + attempt
                    + "/"
                    + maxAttempts,
                exception);
        if (attempt == maxAttempts) {
          throw lastFailure;
        }
      }

      waitBeforeRetry(searchDate, attempt);
    }
    throw lastFailure;
  }

  private URI buildUri(String searchDate) {
    return baseUri.resolve(
        PATH + "?authkey=" + apiKey + "&searchdate=" + searchDate + "&data=AP01");
  }

  private List<EximbankExchangeRateItem> parseRates(
      String responseBody, String searchDate, int attempt) throws IOException {
    try {
      return objectMapper.readValue(
          responseBody,
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, EximbankExchangeRateItem.class));
    } catch (IOException exception) {
      throw new IOException(
          "한국수출입은행 환율 응답 파싱 오류입니다. searchDate="
              + searchDate
              + ", attempt="
              + attempt
              + "/"
              + maxAttempts,
          exception);
    }
  }

  private void validateResultCodes(List<EximbankExchangeRateItem> rates, String searchDate) {
    if (rates.isEmpty() || rates.stream().anyMatch(rate -> rate.result() == 1)) {
      return;
    }
    Set<Integer> resultCodes = new LinkedHashSet<>();
    rates.forEach(rate -> resultCodes.add(rate.result()));
    throw new EximbankApiException(
        "한국수출입은행 환율 API가 실패 결과를 반환했습니다. searchDate="
            + searchDate
            + ", resultCodes="
            + resultCodes);
  }

  private boolean isRetryableStatus(int statusCode) {
    return statusCode == 408 || statusCode == 429 || statusCode >= 500;
  }

  private void waitBeforeRetry(String searchDate, int failedAttempt) {
    Duration retryDelay = initialRetryDelay.multipliedBy(1L << (failedAttempt - 1));
    try {
      retrySleeper.sleep(retryDelay);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new EximbankApiException(
          "한국수출입은행 환율 조회 재시도 대기 중 중단되었습니다. searchDate="
              + searchDate
              + ", attempt="
              + failedAttempt
              + "/"
              + maxAttempts,
          exception);
    }
  }

  @FunctionalInterface
  interface RetrySleeper {

    void sleep(Duration duration) throws InterruptedException;
  }
}
