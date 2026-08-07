package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.investment.exception.KrxApiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KrxIndexClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String KOSPI_PATH = "/svc/apis/idx/kospi_dd_trd";
  private static final String KOSDAQ_PATH = "/svc/apis/idx/kosdaq_dd_trd";

  private final URI baseUri;
  private final String authKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public KrxIndexClient(
      @Value("${krx.api.base-url:https://data-dbg.krx.co.kr}") String baseUrl,
      @Value("${krx.api.auth-key:}") String authKey) {
    this(
        URI.create(baseUrl),
        authKey,
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
        new ObjectMapper());
  }

  KrxIndexClient(
      URI baseUri, String authKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.baseUri = baseUri;
    this.authKey = authKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public KrxIndexDailyTradingResponse getKospiDailyTrading(String basDd) {
    return request(KOSPI_PATH, basDd, "KRX 코스피 지수 시세 조회에 실패했습니다.");
  }

  public KrxIndexDailyTradingResponse getKosdaqDailyTrading(String basDd) {
    return request(KOSDAQ_PATH, basDd, "KRX 코스닥 지수 시세 조회에 실패했습니다.");
  }

  private KrxIndexDailyTradingResponse request(
      String path, String basDd, String failureMessage) {
    if (authKey == null || authKey.isBlank()) {
      throw new KrxApiException(
          "KRX API 인증키가 설정되지 않았습니다. application-local.properties에 krx.api.auth-key를 설정하세요.",
          503);
    }
    try {
      String requestBody = objectMapper.writeValueAsString(Map.of("basDd", basDd));
      HttpRequest request =
          HttpRequest.newBuilder(baseUri.resolve(path))
              .timeout(REQUEST_TIMEOUT)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .header("AUTH_KEY", authKey)
              .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(
              request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new KrxApiException(failureMessage, response.statusCode());
      }
      return objectMapper.readValue(response.body(), KrxIndexDailyTradingResponse.class);
    } catch (KrxApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new KrxApiException(failureMessage + " (요청 중단됨)", 503, exception);
    } catch (Exception exception) {
      throw new KrxApiException(failureMessage, 502, exception);
    }
  }
}
