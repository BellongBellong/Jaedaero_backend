package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.marketreport.model.MarketIndex;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 공공데이터포털 금융위원회_지수시세정보 중 주가지수시세를 조회한다. */
@Component
public class FinancialMarketIndexClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String STOCK_MARKET_INDEX_PATH = "/getStockMarketIndex";
  private static final String NORMAL_RESULT_CODE = "00";
  private static final DateTimeFormatter BASE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final URI baseUri;
  private final String serviceKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public FinancialMarketIndexClient(
      @Value(
              "${market-index.api.base-url:https://apis.data.go.kr/1160100/service/GetMarketIndexInfoService}")
          String baseUrl,
      @Value("${market-index.api.service-key:}") String serviceKey) {
    this(
        URI.create(baseUrl),
        serviceKey,
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
        new ObjectMapper());
  }

  FinancialMarketIndexClient(
      URI baseUri, String serviceKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.baseUri = baseUri;
    this.serviceKey = serviceKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public List<MarketIndex> getStockMarketIndex(String basDt, String idxNm) {
    if (!StringUtils.hasText(serviceKey)) {
      throw new FinancialMarketIndexApiException(
          "금융위원회 지수시세정보 서비스키가 설정되지 않았습니다. "
              + "application-local.properties에 market-index.api.service-key를 설정하세요.");
    }
    if (!StringUtils.hasText(basDt) || !StringUtils.hasText(idxNm)) {
      throw new IllegalArgumentException("주가지수 기준일자와 지수명은 필수입니다.");
    }

    try {
      HttpRequest request =
          HttpRequest.newBuilder(requestUri(basDt, idxNm))
              .timeout(REQUEST_TIMEOUT)
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(
              request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new FinancialMarketIndexApiException(
            "금융위원회 주가지수시세 조회에 실패했습니다. status=" + response.statusCode());
      }
      return parse(response.body());
    } catch (FinancialMarketIndexApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new FinancialMarketIndexApiException(
          "금융위원회 주가지수시세 조회 요청이 중단되었습니다.", exception);
    } catch (Exception exception) {
      throw new FinancialMarketIndexApiException(
          "금융위원회 주가지수시세 조회에 실패했습니다.", exception);
    }
  }

  private URI requestUri(String basDt, String idxNm) {
    String base = baseUri.toString().replaceFirst("/+$", "");
    return URI.create(
        base
            + STOCK_MARKET_INDEX_PATH
            + "?serviceKey="
            + encodeServiceKey(serviceKey)
            + "&resultType=json&pageNo=1&numOfRows=1&basDt="
            + encode(basDt)
            + "&idxNm="
            + encode(idxNm));
  }

  private String encodeServiceKey(String value) {
    if (value.contains("%")) {
      return value;
    }
    return encode(value);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private List<MarketIndex> parse(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode response = root.path("response");
    JsonNode header = response.path("header");
    String resultCode = header.path("resultCode").asText("");
    if (!NORMAL_RESULT_CODE.equals(resultCode)) {
      String resultMessage = header.path("resultMsg").asText("응답 메시지 없음");
      throw new FinancialMarketIndexApiException(
          "금융위원회 주가지수시세 API 오류입니다. code="
              + resultCode
              + ", message="
              + resultMessage);
    }

    JsonNode itemNode = response.path("body").path("items").path("item");
    if (itemNode.isMissingNode() || itemNode.isNull()) {
      return List.of();
    }

    List<MarketIndex> items = new ArrayList<>();
    if (itemNode.isArray()) {
      for (JsonNode item : itemNode) {
        items.add(toMarketIndex(item));
      }
    } else if (itemNode.isObject()) {
      items.add(toMarketIndex(itemNode));
    }
    return List.copyOf(items);
  }

  private MarketIndex toMarketIndex(JsonNode item) {
    FinancialMarketIndexItem externalItem =
        new FinancialMarketIndexItem(
            item.path("basDt").asText(""),
            item.path("idxNm").asText(""),
            item.path("clpr").asText(""),
            item.path("vs").asText(""),
            item.path("fltRt").asText(""));
    return new MarketIndex(
        LocalDate.parse(externalItem.basDt(), BASE_DATE_FORMAT),
        externalItem.idxNm(),
        new BigDecimal(externalItem.clpr()),
        new BigDecimal(externalItem.vs()),
        new BigDecimal(externalItem.fltRt()));
  }
}
