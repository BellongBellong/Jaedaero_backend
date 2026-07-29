package com.jaedaero.domain.investment.etf;

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
public class KrxEtfClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String ETF_DAILY_TRADING_PATH = "/svc/apis/etp/etf_bydd_trd";

  private final URI baseUri;
  private final String authKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public KrxEtfClient(
      @Value("${krx.api.base-url:https://data-dbg.krx.co.kr}") String baseUrl,
      @Value("${krx.api.auth-key:}") String authKey) {
    this(
        URI.create(baseUrl),
        authKey,
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
        new ObjectMapper());
  }

  KrxEtfClient(URI baseUri, String authKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.baseUri = baseUri;
    this.authKey = authKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public EtfDailyTradingResponse getDailyTrading(String basDd) {
    if (authKey == null || authKey.isBlank()) {
      throw new KrxApiException(
          "KRX API 인증키가 설정되지 않았습니다. application-local.properties에 krx.api.auth-key를 설정하세요.", 503);
    }
    try {
      String requestBody = objectMapper.writeValueAsString(Map.of("basDd", basDd));
      HttpRequest request =
          HttpRequest.newBuilder(baseUri.resolve(ETF_DAILY_TRADING_PATH))
              .timeout(REQUEST_TIMEOUT)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .header("AUTH_KEY", authKey)
              .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new KrxApiException("KRX ETF 일별매매정보 조회에 실패했습니다.", response.statusCode());
      }
      return objectMapper.readValue(response.body(), EtfDailyTradingResponse.class);
    } catch (KrxApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new KrxApiException("KRX ETF 일별매매정보 요청이 중단되었습니다.", 503, exception);
    } catch (Exception exception) {
      throw new KrxApiException("KRX ETF 일별매매정보 조회에 실패했습니다.", 502, exception);
    }
  }
}
