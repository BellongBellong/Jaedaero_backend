package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Finnhub Market News API에서 최신 시장 뉴스 후보를 조회한다. */
@Component
public class FinnhubMarketNewsClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String NEWS_PATH = "/api/v1/news";
  private static final Set<String> SUPPORTED_CATEGORIES =
      Set.of("general", "forex", "crypto", "merger");

  private final URI baseUri;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public FinnhubMarketNewsClient(
      @Value("${finnhub.api.base-url:https://finnhub.io}") String baseUrl,
      @Value("${finnhub.api.key:}") String apiKey) {
    this(
        URI.create(baseUrl),
        apiKey,
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
        new ObjectMapper());
  }

  FinnhubMarketNewsClient(
      URI baseUri, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.baseUri = baseUri;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public List<FinnhubNewsItem> getMarketNews(String category) {
    if (!StringUtils.hasText(apiKey)) {
      throw new FinnhubApiException(
          "Finnhub API 인증키가 설정되지 않았습니다. application-local.properties에 finnhub.api.key를 설정하세요.");
    }
    if (!SUPPORTED_CATEGORIES.contains(category)) {
      throw new IllegalArgumentException("지원하지 않는 Finnhub 뉴스 category입니다: " + category);
    }

    try {
      String base = baseUri.toString().replaceFirst("/+$", "");
      URI uri =
          URI.create(
              base
                  + NEWS_PATH
                  + "?category="
                  + encode(category)
                  + "&token="
                  + encode(apiKey));
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
        throw new FinnhubApiException(
            "Finnhub 시장 뉴스 조회에 실패했습니다. category="
                + category
                + ", status="
                + response.statusCode());
      }
      FinnhubNewsItem[] items = objectMapper.readValue(response.body(), FinnhubNewsItem[].class);
      return items == null ? List.of() : List.copyOf(Arrays.asList(items));
    } catch (FinnhubApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new FinnhubApiException("Finnhub 시장 뉴스 조회 요청이 중단되었습니다.", exception);
    } catch (Exception exception) {
      throw new FinnhubApiException("Finnhub 시장 뉴스 조회에 실패했습니다.", exception);
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
