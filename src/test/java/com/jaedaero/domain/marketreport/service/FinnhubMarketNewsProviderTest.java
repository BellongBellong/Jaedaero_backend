package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.client.FinnhubApiException;
import com.jaedaero.domain.marketreport.client.FinnhubMarketNewsClient;
import com.jaedaero.domain.marketreport.client.FinnhubNewsItem;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinnhubMarketNewsProviderTest {

  private static final Instant AS_OF = Instant.parse("2026-08-11T08:00:00Z");

  @Test
  void keepsGeneralNewsWhenForexRequestFails() {
    FinnhubNewsItem general = item(1, "general");
    RecordingSelector selector = new RecordingSelector();

    new FinnhubMarketNewsProvider(
            new StubFinnhubClient(List.of(general), new FinnhubApiException("forex failed")),
            selector)
        .fetchImportantNews(AS_OF);

    assertEquals(List.of(general), selector.generalNews);
    assertEquals(List.of(), selector.forexNews);
  }

  @Test
  void keepsForexNewsWhenGeneralRequestFails() {
    FinnhubNewsItem forex = item(2, "forex");
    RecordingSelector selector = new RecordingSelector();

    new FinnhubMarketNewsProvider(
            new StubFinnhubClient(new FinnhubApiException("general failed"), List.of(forex)),
            selector)
        .fetchImportantNews(AS_OF);

    assertEquals(List.of(), selector.generalNews);
    assertEquals(List.of(forex), selector.forexNews);
  }

  @Test
  void propagatesCombinedFinnhubFailureOnlyWhenBothCategoriesFail() {
    FinnhubApiException generalFailure = new FinnhubApiException("general failed");
    FinnhubApiException forexFailure = new FinnhubApiException("forex failed");

    FinnhubApiException combined =
        assertThrows(
            FinnhubApiException.class,
            () ->
                new FinnhubMarketNewsProvider(
                        new StubFinnhubClient(generalFailure, forexFailure),
                        new RecordingSelector())
                    .fetchImportantNews(AS_OF));

    assertSame(generalFailure, combined.getCause());
    assertEquals(1, combined.getSuppressed().length);
    assertSame(forexFailure, combined.getSuppressed()[0]);
  }

  private FinnhubNewsItem item(long id, String category) {
    return new FinnhubNewsItem(
        category,
        AS_OF.minusSeconds(3600).getEpochSecond(),
        "시장 뉴스 " + id,
        id,
        "",
        "",
        "Reuters",
        "요약",
        "https://example.com/news/" + id);
  }

  private static class RecordingSelector extends ImportantMarketNewsSelector {
    private List<FinnhubNewsItem> generalNews;
    private List<FinnhubNewsItem> forexNews;

    @Override
    public List<MarketNewsArticle> select(
        List<FinnhubNewsItem> generalNews, List<FinnhubNewsItem> forexNews, Instant asOf) {
      this.generalNews = generalNews;
      this.forexNews = forexNews;
      return List.of();
    }
  }

  private static class StubFinnhubClient extends FinnhubMarketNewsClient {
    private final List<FinnhubNewsItem> generalNews;
    private final List<FinnhubNewsItem> forexNews;
    private final FinnhubApiException generalFailure;
    private final FinnhubApiException forexFailure;

    StubFinnhubClient(
        List<FinnhubNewsItem> generalNews, FinnhubApiException forexFailure) {
      super("https://example.com", "test-key");
      this.generalNews = generalNews;
      this.forexNews = List.of();
      this.generalFailure = null;
      this.forexFailure = forexFailure;
    }

    StubFinnhubClient(
        FinnhubApiException generalFailure, List<FinnhubNewsItem> forexNews) {
      super("https://example.com", "test-key");
      this.generalNews = List.of();
      this.forexNews = forexNews;
      this.generalFailure = generalFailure;
      this.forexFailure = null;
    }

    StubFinnhubClient(
        FinnhubApiException generalFailure, FinnhubApiException forexFailure) {
      super("https://example.com", "test-key");
      this.generalNews = List.of();
      this.forexNews = List.of();
      this.generalFailure = generalFailure;
      this.forexFailure = forexFailure;
    }

    @Override
    public List<FinnhubNewsItem> getMarketNews(String category) {
      if ("general".equals(category) && generalFailure != null) {
        throw generalFailure;
      }
      if ("forex".equals(category) && forexFailure != null) {
        throw forexFailure;
      }
      return "general".equals(category) ? generalNews : forexNews;
    }
  }
}
