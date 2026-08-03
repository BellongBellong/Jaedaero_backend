package com.jaedaero.domain.aianalysis.llm;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class OpenAiChatCompletionsNarrativeGeneratorTest {

  @Test
  void sendsStrictJsonSchemaAndParsesNarrative() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    OpenAiChatCompletionsNarrativeGenerator generator =
        new OpenAiChatCompletionsNarrativeGenerator(restTemplate, new ObjectMapper());
    ReflectionTestUtils.setField(generator, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(generator, "chatCompletionsUrl", "https://llm.example/api/chat/completions");

    server
        .expect(requestTo("https://llm.example/api/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-key"))
        .andExpect(content().string(containsString("\"response_format\"")))
        .andExpect(content().string(containsString("\"json_schema\"")))
        .andExpect(content().string(containsString("\"strict\":true")))
        .andExpect(content().string(containsString("\"additionalProperties\":false")))
        .andRespond(
            withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"comment\\\":\\\"분석 코멘트\\\",\\\"recommendReason\\\":\\\"추천 사유\\\"}\"}}]}",
                MediaType.APPLICATION_JSON));

    AiCoachNarrative result = generator.generate(OpenAiModel.GPT_4O_MINI, "목 데이터 기반 분석");

    assertEquals("분석 코멘트", result.comment());
    assertEquals("추천 사유", result.recommendReason());
    server.verify();
  }
}
