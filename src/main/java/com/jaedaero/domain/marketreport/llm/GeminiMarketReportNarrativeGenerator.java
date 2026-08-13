package com.jaedaero.domain.marketreport.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/** Finnhub 뉴스 근거를 전달받아 Gemini Developer API generateContent로 시장 리포트를 생성한다. */
@Component
@Slf4j
public class GeminiMarketReportNarrativeGenerator implements MarketReportNarrativeGenerator {

  public static final String MODEL_NAME = "gemini-3.6-flash";
  public static final int MINIMUM_SOURCE_COUNT = 2;
  public static final int MINIMUM_CONTENT_CHAR_COUNT = 300;
  static final String DEFAULT_GENERATE_CONTENT_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/"
          + MODEL_NAME
          + ":generateContent";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String generateContentUrl;
  private final int maxAttempts;
  private final long retryInitialDelayMs;

  @Autowired
  public GeminiMarketReportNarrativeGenerator(
      @Qualifier("geminiRestTemplate") RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Value("${gemini.api-key:}") String apiKey,
      @Value("${gemini.generate-content-url:" + DEFAULT_GENERATE_CONTENT_URL + "}")
          String generateContentUrl,
      @Value("${gemini.max-attempts:4}") int maxAttempts,
      @Value("${gemini.retry-initial-delay-ms:1500}") long retryInitialDelayMs) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.generateContentUrl = generateContentUrl;
    this.maxAttempts = Math.max(1, maxAttempts);
    this.retryInitialDelayMs = Math.max(0L, retryInitialDelayMs);
  }

  /** 단위 테스트와 독립 실행용 생성자. 재시도 동작은 명시적인 전체 생성자로 검증한다. */
  public GeminiMarketReportNarrativeGenerator(
      RestTemplate restTemplate,
      ObjectMapper objectMapper,
      String apiKey,
      String generateContentUrl) {
    this(restTemplate, objectMapper, apiKey, generateContentUrl, 1, 0L);
  }

  @Override
  public MarketReportNarrative generate(String prompt) {
    return generate(prompt, List.of());
  }

  @Override
  public MarketReportNarrative generate(
      String prompt, List<MarketReportSourceItem> availableSources) {
    if (!StringUtils.hasText(apiKey)) {
      throw new AiCoachNarrativeGenerationException("gemini.api-key가 설정되지 않았습니다.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.set("x-goog-api-key", apiKey);

    Map<String, Object> requestBody =
        Map.of(
            "contents",
            List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig",
            generationConfig());

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        ResponseEntity<String> response =
            restTemplate.exchange(
                generateContentUrl,
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                String.class);
        return parse(response.getBody(), availableSources);
      } catch (RestClientResponseException exception) {
        if (isRetryableStatus(exception.getRawStatusCode()) && attempt < maxAttempts) {
          waitBeforeRetry(attempt, "HTTP " + exception.getRawStatusCode());
          continue;
        }
        throw new AiCoachNarrativeGenerationException(
            "Gemini generateContent 호출이 HTTP "
                + exception.getRawStatusCode()
                + "로 실패했습니다. "
                + safeHttpErrorDetail(exception),
            exception);
      } catch (RestClientException exception) {
        if (attempt < maxAttempts) {
          waitBeforeRetry(attempt, exception.getClass().getSimpleName());
          continue;
        }
        throw new AiCoachNarrativeGenerationException(
            "Gemini generateContent 호출에 실패했습니다. " + safeText(exception.getMessage()),
            exception);
      } catch (IllegalArgumentException exception) {
        throw new AiCoachNarrativeGenerationException(
            "Gemini generateContent URL이 올바르지 않습니다. "
                + safeText(exception.getMessage()),
            exception);
      }
    }
    throw new AiCoachNarrativeGenerationException("Gemini generateContent 재시도가 종료되었습니다.");
  }

  private boolean isRetryableStatus(int statusCode) {
    return statusCode == 408
        || statusCode == 429
        || statusCode == 500
        || statusCode == 502
        || statusCode == 503
        || statusCode == 504;
  }

  private void waitBeforeRetry(int failedAttempt, String reason) {
    long delayMs = retryInitialDelayMs * (1L << Math.min(failedAttempt - 1, 10));
    log.warn(
        "Gemini 일시적 호출 실패로 재시도합니다. attempt={}/{}, delayMs={}, reason={}",
        failedAttempt,
        maxAttempts,
        delayMs,
        reason);
    if (delayMs == 0L) {
      return;
    }
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AiCoachNarrativeGenerationException(
          "Gemini generateContent 재시도 대기가 중단되었습니다.", exception);
    }
  }

  private MarketReportNarrative parse(
      String responseBody, List<MarketReportSourceItem> availableSources) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode candidates = root.path("candidates");
      if (!candidates.isArray() || candidates.isEmpty()) {
        throw new AiCoachNarrativeGenerationException("Gemini 응답에 candidates가 없습니다.");
      }
      AiCoachNarrativeGenerationException lastFailure = null;
      for (JsonNode candidate : candidates) {
        try {
          return parseCandidate(parseGenerateContentCandidate(candidate), availableSources);
        } catch (AiCoachNarrativeGenerationException exception) {
          lastFailure = exception;
        }
      }
      throw lastFailure;
    } catch (AiCoachNarrativeGenerationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답을 시장 리포트로 해석하지 못했습니다. "
              + exception.getClass().getSimpleName()
              + ": "
              + safeText(exception.getMessage()),
          exception);
    }
  }

  private ParsedOutput parseGenerateContentCandidate(JsonNode candidate) {
    StringBuilder text = new StringBuilder();
    JsonNode parts = candidate.path("content").path("parts");
    if (parts.isArray()) {
      for (JsonNode part : parts) {
        String partText = part.path("text").asText("").trim();
        if (StringUtils.hasText(partText)) {
          if (text.length() > 0) {
            text.append('\n');
          }
          text.append(partText);
        }
      }
    }
    if (!StringUtils.hasText(text)) {
      throw new AiCoachNarrativeGenerationException("Gemini 응답 candidate에 본문이 없습니다.");
    }
    return new ParsedOutput(text.toString());
  }

  private MarketReportNarrative parseCandidate(
      ParsedOutput output, List<MarketReportSourceItem> availableSources) throws Exception {
    if (!StringUtils.hasText(output.content())) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답 candidate에 본문이 없습니다.");
    }
    JsonNode narrative = objectMapper.readTree(removeMarkdownFence(output.content()));
    if (narrative == null || !narrative.isObject()) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답 본문이 구조화된 JSON 객체가 아닙니다.");
    }
    String title = requiredText(narrative, "title");
    String summary = requiredText(narrative, "summary", "oneLineSummary");
    String content = requiredText(narrative, "content");
    List<MarketReportSourceItem> usedSources =
        parseUsedSources(narrative.path("sourceIds"), availableSources);
    if (title.codePointCount(0, title.length()) > MarketReportNarrative.MAX_TITLE_CHAR_COUNT
        || summary.codePointCount(0, summary.length())
            > MarketReportNarrative.MAX_SUMMARY_CHAR_COUNT) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답의 title 또는 summary가 저장 가능한 길이를 초과했습니다.");
    }
    if (content.codePointCount(0, content.length()) < MINIMUM_CONTENT_CHAR_COUNT
        || !containsKorean(content)) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답의 content가 한국어 300자 이상이 아닙니다.");
    }
    return new MarketReportNarrative(title, summary, content, usedSources);
  }

  private Map<String, Object> generationConfig() {
    Map<String, Object> schema =
        Map.of(
            "type",
            "OBJECT",
            "properties",
            Map.of(
                "title", Map.of("type", "STRING"),
                "summary", Map.of("type", "STRING"),
                "content", Map.of("type", "STRING"),
                "sourceIds", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
            "required",
            List.of("title", "summary", "content", "sourceIds"),
            "propertyOrdering",
            List.of("title", "summary", "content", "sourceIds"));
    return Map.of("responseMimeType", "application/json", "responseSchema", schema);
  }

  private String requiredText(JsonNode source, String... fieldNames) {
    for (String fieldName : fieldNames) {
      JsonNode value = source.get(fieldName);
      if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
        return value.asText().trim();
      }
    }
    throw new AiCoachNarrativeGenerationException(
        "Gemini 구조화 응답의 필수 필드가 비어 있습니다: " + String.join(" 또는 ", fieldNames));
  }

  private String removeMarkdownFence(String value) {
    String trimmed = value == null ? "" : value.trim();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }
    int firstLineBreak = trimmed.indexOf('\n');
    int lastFence = trimmed.lastIndexOf("```");
    if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
      return trimmed;
    }
    return trimmed.substring(firstLineBreak + 1, lastFence).trim();
  }

  private boolean containsKorean(String content) {
    return content.codePoints().anyMatch(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3);
  }

  private List<MarketReportSourceItem> parseUsedSources(
      JsonNode sourceIds, List<MarketReportSourceItem> availableSources) {
    if (!sourceIds.isArray() || availableSources == null) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답에 Finnhub 사용 출처 ID 배열이 없습니다.");
    }
    Set<Integer> indexes = new LinkedHashSet<>();
    for (JsonNode sourceId : sourceIds) {
      if (!sourceId.isTextual() || !sourceId.asText().matches("N[1-9][0-9]*")) {
        throw new AiCoachNarrativeGenerationException(
            "Gemini 응답에 허용되지 않은 Finnhub 출처 ID가 있습니다.");
      }
      int index = Integer.parseInt(sourceId.asText().substring(1)) - 1;
      if (index < 0 || index >= availableSources.size()) {
        throw new AiCoachNarrativeGenerationException(
            "Gemini 응답이 제공되지 않은 Finnhub 출처를 참조했습니다.");
      }
      indexes.add(index);
    }
    if (indexes.size() < MINIMUM_SOURCE_COUNT) {
      throw new AiCoachNarrativeGenerationException(
          "Gemini 응답이 사용한 유효 Finnhub 출처가 2개 미만입니다.");
    }
    List<MarketReportSourceItem> sources = new ArrayList<>();
    for (Integer index : indexes) {
      sources.add(availableSources.get(index));
    }
    return List.copyOf(sources);
  }

  private String safeHttpErrorDetail(RestClientResponseException exception) {
    String detail = "";
    String responseBody = exception.getResponseBodyAsString();
    if (StringUtils.hasText(responseBody)) {
      try {
        JsonNode error = objectMapper.readTree(responseBody).path("error");
        if (error.isObject()) {
          String code = error.path("code").asText("").trim();
          String status = error.path("status").asText("").trim();
          String message = error.path("message").asText("").trim();
          detail =
              List.of(code, status, message).stream()
                  .filter(StringUtils::hasText)
                  .reduce((left, right) -> left + ": " + right)
                  .orElse("");
        }
      } catch (Exception ignored) {
        // 오류 응답 전문은 로그에 남기지 않고 상태 코드만 기록한다.
      }
    }
    if (!StringUtils.hasText(detail)) {
      detail = exception.getStatusText();
    }
    return safeText(detail);
  }

  private String safeText(String value) {
    if (!StringUtils.hasText(value)) {
      return "상세 오류 메시지 없음";
    }
    String sanitized = StringUtils.hasText(apiKey) ? value.replace(apiKey, "[REDACTED]") : value;
    int maxLength = 500;
    return sanitized.length() <= maxLength
        ? sanitized
        : sanitized.substring(0, maxLength) + "…";
  }

  private record ParsedOutput(String content) {}
}
