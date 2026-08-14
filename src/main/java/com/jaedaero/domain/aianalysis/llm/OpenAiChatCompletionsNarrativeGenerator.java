package com.jaedaero.domain.aianalysis.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
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

/** OpenAI Chat Completions 호환 프록시를 통해 AI 코치 문구를 생성한다. */
@Primary
@Component
@RequiredArgsConstructor
public class OpenAiChatCompletionsNarrativeGenerator implements AiCoachNarrativeGenerator {

  private static final String DEFAULT_CHAT_COMPLETIONS_URL =
      "https://port-0-llm-proxy-node-mrlu0vim5f938376.sel3.cloudtype.app/v1/chat/completions";
  private static final String SYSTEM_MESSAGE =
      "당신은 한국 군 장병을 위한 금융 코치입니다. 숫자 계산이나 투자 판단을 새로 하지 말고, "
          + "입력으로 받은 확정 결과만 자연스럽고 신중한 한국어로 설명하세요. "
          + "개인화된 투자 자문·수익 보장을 하지 말고, 두 문구는 각각 2문장 이내로 작성하세요. "
          + "recommendReason에서는 금액을 다시 계산하거나 언급하지 말고 실행 방법만 설명하세요. "
          + "응답 스키마의 comment와 recommendReason 필드만 채우세요.";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${llm.api-key:}")
  private String apiKey;

  @Value("${llm.chat-completions-url:" + DEFAULT_CHAT_COMPLETIONS_URL + "}")
  private String chatCompletionsUrl;

  @Override
  public AiCoachNarrative generate(OpenAiModel model, String prompt) {
    if (!StringUtils.hasText(apiKey)) {
      throw new AiCoachNarrativeGenerationException("llm.api-key가 설정되지 않았습니다.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> requestBody =
        Map.of(
            "model", model.apiName(),
            "messages",
            List.of(
                Map.of("role", "system", "content", SYSTEM_MESSAGE),
                Map.of("role", "user", "content", prompt)),
            "response_format",
            Map.of(
                "type", "json_schema",
                "json_schema",
                Map.of(
                    "name", "ai_consumption_narrative_v6",
                    "strict", true,
                    "schema",
                    Map.of(
                        "type", "object",
                        "properties",
                        Map.of(
                            "comment", Map.of("type", "string"),
                            "recommendReason", Map.of("type", "string")),
                        "required", List.of("comment", "recommendReason"),
                        "additionalProperties", false))));

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              chatCompletionsUrl,
              HttpMethod.POST,
              new HttpEntity<>(requestBody, headers),
              String.class);
      return parse(response.getBody());
    } catch (RestClientResponseException e) {
      throw new AiCoachNarrativeGenerationException(
          "OpenAI Chat Completions 호출이 HTTP " + e.getRawStatusCode() + "로 실패했습니다.");
    } catch (RestClientException e) {
      throw new AiCoachNarrativeGenerationException("OpenAI Chat Completions 호출에 실패했습니다.", e);
    }
  }

  private AiCoachNarrative parse(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (!content.isTextual()) {
        throw new AiCoachNarrativeGenerationException("OpenAI 응답에 choices[0].message.content가 없습니다.");
      }

      String generatedJson = removeMarkdownFence(content.asText());
      JsonNode narrative = objectMapper.readTree(generatedJson);
      String comment = requiredText(narrative, "comment");
      String recommendReason = requiredText(narrative, "recommendReason");
      return new AiCoachNarrative(comment, recommendReason);
    } catch (AiCoachNarrativeGenerationException e) {
      throw e;
    } catch (Exception e) {
      throw new AiCoachNarrativeGenerationException("OpenAI 응답을 AI 코치 문구로 해석하지 못했습니다.", e);
    }
  }

  private String requiredText(JsonNode source, String field) {
    JsonNode value = source.path(field);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw new AiCoachNarrativeGenerationException("OpenAI 응답의 " + field + " 값이 비어 있습니다.");
    }
    return value.asText().trim();
  }

  private String removeMarkdownFence(String content) {
    String trimmed = content.trim();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }
    return trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
  }
}
