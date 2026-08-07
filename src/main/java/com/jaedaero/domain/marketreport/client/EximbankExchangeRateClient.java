package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EximbankExchangeRateClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String PATH = "/site/program/financial/exchangeJSON";

  private final URI baseUri;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

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
    this.baseUri = baseUri;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public List<EximbankExchangeRateItem> getExchangeRates(String searchDate) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new EximbankApiException(
          "한국수출입은행 API 인증키가 설정되지 않았습니다. application-local.properties에 eximbank.api.key를 설정하세요.");
    }
    try {
      URI uri =
          baseUri.resolve(
              PATH
                  + "?authkey="
                  + apiKey
                  + "&searchdate="
                  + searchDate
                  + "&data=AP01");
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
        throw new EximbankApiException(
            "한국수출입은행 환율 조회에 실패했습니다. status=" + response.statusCode());
      }
      return objectMapper.readValue(
          response.body(),
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, EximbankExchangeRateItem.class));
    } catch (EximbankApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new EximbankApiException("한국수출입은행 환율 조회 요청이 중단되었습니다.", exception);
    } catch (Exception exception) {
      throw new EximbankApiException("한국수출입은행 환율 조회에 실패했습니다.", exception);
    }
  }
}
