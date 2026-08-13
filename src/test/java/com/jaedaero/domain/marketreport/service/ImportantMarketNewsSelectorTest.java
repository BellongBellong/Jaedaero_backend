package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.marketreport.client.FinnhubNewsItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportantMarketNewsSelectorTest {

  private static final Instant AS_OF = Instant.parse("2026-08-11T08:00:00Z");

  private final ImportantMarketNewsSelector selector = new ImportantMarketNewsSelector();

  @Test
  void prioritizesMacroRelevanceAndKeepsForexDiversity() {
    List<FinnhubNewsItem> general =
        List.of(
            item(
                1,
                "general",
                10,
                "Federal Reserve decision lifts Treasury yield and dollar",
                "Reuters",
                "https://example.com/fed"),
            item(
                2,
                "general",
                1,
                "Celebrity launches a new lifestyle brand",
                "Unknown Blog",
                "https://example.com/lifestyle"),
            item(
                3,
                "general",
                4,
                "South Korea semiconductor exports influence stock market",
                "Bloomberg",
                "https://example.com/korea"));
    List<FinnhubNewsItem> forex =
        List.of(
            item(
                4,
                "forex",
                3,
                "Korean won moves against dollar after central bank remarks",
                "Reuters",
                "https://example.com/won"),
            item(
                5,
                "forex",
                5,
                "Euro and dollar react to inflation data",
                "CNBC",
                "https://example.com/euro"));

    List<MarketNewsArticle> selected = selector.select(general, forex, AS_OF);

    assertTrue(
        selected.indexOf(selected.stream().filter(article -> article.id() == 1).findFirst().orElseThrow())
            < selected.indexOf(
                selected.stream().filter(article -> article.id() == 2).findFirst().orElseThrow()));
    assertTrue(selected.stream().anyMatch(article -> article.id() == 4));
    assertTrue(selected.stream().anyMatch(article -> article.id() == 5));
    assertTrue(
        selected.stream().filter(article -> "forex".equals(article.category())).count() >= 2);
  }

  @Test
  void removesOldInvalidAndDuplicateNewsAndCapsResult() {
    List<FinnhubNewsItem> general = new ArrayList<>();
    general.add(
        item(
            1,
            "general",
            2,
            "Inflation changes the interest rate outlook",
            "Reuters",
            "https://example.com/shared"));
    general.add(
        item(
            2,
            "general",
            3,
            "Inflation changes the interest rate outlook",
            "Reuters",
            "https://example.org/duplicate-headline"));
    general.add(
        item(
            3,
            "general",
            49,
            "Old Federal Reserve story",
            "Reuters",
            "https://example.com/old"));
    general.add(
        item(
            4,
            "general",
            2,
            "Invalid URL story",
            "Reuters",
            "javascript:alert(1)"));
    for (int index = 5; index < 20; index++) {
      general.add(
          item(
              index,
              "general",
              index % 20,
              "Distinct market story " + index,
              "Source " + index,
              "https://example.com/story-" + index));
    }

    List<MarketNewsArticle> selected = selector.select(general, List.of(), AS_OF);

    assertTrue(selected.size() <= ImportantMarketNewsSelector.MAX_SELECTED_NEWS_COUNT);
    assertEquals(1, selected.stream().filter(article -> article.url().contains("shared")).count());
    assertFalse(selected.stream().anyMatch(article -> article.id() == 2));
    assertFalse(selected.stream().anyMatch(article -> article.id() == 3));
    assertFalse(selected.stream().anyMatch(article -> article.id() == 4));
  }

  private FinnhubNewsItem item(
      long id,
      String category,
      long ageHours,
      String headline,
      String source,
      String url) {
    return new FinnhubNewsItem(
        category,
        AS_OF.minusSeconds(ageHours * 3600).getEpochSecond(),
        headline,
        id,
        "",
        "SPY",
        source,
        headline + " summary",
        url);
  }
}
