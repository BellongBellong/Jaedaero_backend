package com.jaedaero.domain.marketreport.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.aianalysis.llm.OpenAiModel;
import com.jaedaero.domain.marketreport.dto.MarketCondition;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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

/** OpenAI Chat Completions 호환 프록시로 공통 시장 요약을 생성한다. */
@Component
public class OpenAiMarketReportNarrativeGenerator
    implements MarketReportNarrativeGenerator {

  private static final String DEFAULT_CHAT_COMPLETIONS_URL =
      "https://port-0-llm-proxy-node-mrlu0vim5f938376.sel3.cloudtype.app/v1/chat/completions";
  private static final String SYSTEM_MESSAGE =
      "당신은 한국 군 장병을 위한 금융 서비스의 시장 요약 작성자입니다. 입력으로 받은 "
          + "KOSPI/KOSDAQ/미국채10년물/원달러환율 확정 수치만 근거로 오늘의 시장 상황을 "
          + "BULL/BEAR/NEUTRAL 중 하나로 판단하고, 2문장 이내의 담백한 한국어로 요약하세요. "
          + "개인화된 투자 자문·수익 보장을 하지 마세요.";

  private final RestTemplate restTemplate;
  private final String apiKey;
  private final String chatCompletionsUrl;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  public OpenAiMarketReportNarrativeGenerator(
      RestTemplate restTemplate,
      @Value("${llm.api-key:}") String apiKey,
      @Value("${llm.chat-completions-url:" + DEFAULT_CHAT_COMPLETIONS_URL + "}")
          String chatCompletionsUrl) {
    this.restTemplate = restTemplate;
    this.apiKey = apiKey;
    this.chatCompletionsUrl = chatCompletionsUrl;
  }

  @Override
  public MarketReportNarrative generate(OpenAiModel model, String prompt) {
    if (!StringUtils.hasText(apiKey)) {
      throw new AiCoachNarrativeGenerationException("llm.api-key가 설정되지 않았습니다.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> requestBody =
        Map.of(
            "model",
            model.apiName(),
            "messages",
            List.of(
                Map.of("role", "system", "content", SYSTEM_MESSAGE),
                Map.of("role", "user", "content", prompt)),
            "response_format",
            Map.of(
                "type",
                "json_schema",
                "json_schema",
                Map.of(
                    "name",
                    "daily_market_report_narrative_v1",
                    "strict",
                    true,
                    "schema",
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                            "marketCondition",
                            Map.of(
                                "type",
                                "string",
                                "enum",
                                List.of("BULL", "BEAR", "NEUTRAL")),
                            "content",
                            Map.of("type", "string")),
                        "required",
                        List.of("marketCondition", "content"),
                        "additionalProperties",
                        false))));

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              chatCompletionsUrl,
              HttpMethod.POST,
              new HttpEntity<>(requestBody, headers),
              String.class);
      return parse(response.getBody());
    } catch (RestClientResponseException exception) {
      throw new AiCoachNarrativeGenerationException(
          "OpenAI Chat Completions 호출이 HTTP "
              + exception.getRawStatusCode()
              + "로 실패했습니다.",
          exception);
    } catch (RestClientException exception) {
      throw new AiCoachNarrativeGenerationException(
          "OpenAI Chat Completions 호출에 실패했습니다.", exception);
    }
  }

  private MarketReportNarrative parse(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (!content.isTextual()) {
        throw new AiCoachNarrativeGenerationException(
            "OpenAI 응답에 choices[0].message.content가 없습니다.");
      }
      JsonNode narrative =
          objectMapper.readTree(removeMarkdownFence(content.asText()));
      return new MarketReportNarrative(
          MarketCondition.valueOf(requiredText(narrative, "marketCondition")),
          requiredText(narrative, "content"));
    } catch (AiCoachNarrativeGenerationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AiCoachNarrativeGenerationException(
          "OpenAI 응답을 시장 요약으로 해석하지 못했습니다.", exception);
    }
  }

  private String requiredText(JsonNode source, String field) {
    JsonNode value = source.path(field);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw new AiCoachNarrativeGenerationException(
          "OpenAI 응답의 " + field + " 값이 비어 있습니다.");
    }
    return value.asText().trim();
  }

  private String removeMarkdownFence(String content) {
    String trimmed = content.trim();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }
    return trimmed
        .replaceFirst("^```(?:json)?\\s*", "")
        .replaceFirst("\\s*```$", "")
        .trim();
  }
}
