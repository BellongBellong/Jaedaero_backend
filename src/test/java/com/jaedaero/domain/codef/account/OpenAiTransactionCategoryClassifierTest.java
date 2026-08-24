package com.jaedaero.domain.codef.account;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.codef.persistence.StoredTransactionCategoryCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class OpenAiTransactionCategoryClassifierTest {

  @Test
  void sendsGpt5NanoStrictSchemaAndParsesAllCandidates() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    OpenAiTransactionCategoryClassifier classifier =
        classifier(restTemplate, "https://llm.example/v1/chat/completions");

    server
        .expect(requestTo("https://llm.example/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-key"))
        .andExpect(content().string(containsString("\"model\":\"gpt-5-nano\"")))
        .andExpect(content().string(containsString("transaction_category_batch_v1")))
        .andExpect(content().string(containsString("\"strict\":true")))
        .andExpect(content().string(containsString("ASSET")))
        .andExpect(content().string(containsString("\\\"transactionId\\\":101")))
        .andExpect(content().string(containsString("배달의민족")))
        .andRespond(
            withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"classifications\\\":[{\\\"transactionId\\\":101,\\\"category\\\":\\\"FOOD\\\"},{\\\"transactionId\\\":102,\\\"category\\\":\\\"TRANSPORT\\\"}]}\"}}]}",
                MediaType.APPLICATION_JSON));

    List<TransactionCategoryClassification> result =
        classifier.classify(
            List.of(
                new StoredTransactionCategoryCandidate(101L, "배달의민족"),
                new StoredTransactionCategoryCandidate(102L, "서울교통공사")));

    assertEquals(
        List.of(
            new TransactionCategoryClassification(101L, TransactionCategory.FOOD),
            new TransactionCategoryClassification(102L, TransactionCategory.TRANSPORT)),
        result);
    server.verify();
  }

  @Test
  void rejectsResponseWhenAnyCandidateIsMissing() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    OpenAiTransactionCategoryClassifier classifier =
        classifier(restTemplate, "https://llm.example/v1/chat/completions");
    server
        .expect(requestTo("https://llm.example/v1/chat/completions"))
        .andRespond(
            withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"classifications\\\":[{\\\"transactionId\\\":101,\\\"category\\\":\\\"FOOD\\\"}]}\"}}]}",
                MediaType.APPLICATION_JSON));

    assertThrows(
        TransactionCategoryClassificationException.class,
        () ->
            classifier.classify(
                List.of(
                    new StoredTransactionCategoryCandidate(101L, "배달의민족"),
                    new StoredTransactionCategoryCandidate(102L, "서울교통공사"))));
    server.verify();
  }

  private OpenAiTransactionCategoryClassifier classifier(RestTemplate restTemplate, String url) {
    OpenAiTransactionCategoryClassifier classifier =
        new OpenAiTransactionCategoryClassifier(restTemplate, new ObjectMapper());
    ReflectionTestUtils.setField(classifier, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(classifier, "chatCompletionsUrl", url);
    return classifier;
  }
}
