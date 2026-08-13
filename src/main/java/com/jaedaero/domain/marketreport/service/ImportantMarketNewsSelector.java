package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.FinnhubNewsItem;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 최신성·거시시장 관련성·매체 신뢰도·카테고리 다양성을 기준으로 핵심 뉴스를 고른다. */
@Component
public class ImportantMarketNewsSelector {

  static final int MAX_SELECTED_NEWS_COUNT = 8;
  private static final int MIN_FOREX_NEWS_COUNT = 2;
  private static final int MAX_NEWS_PER_SOURCE = 3;
  private static final Duration MAX_NEWS_AGE = Duration.ofHours(48);
  private static final Duration FUTURE_CLOCK_TOLERANCE = Duration.ofMinutes(10);

  private static final Map<String, Integer> KEYWORD_WEIGHTS = keywordWeights();
  private static final Set<String> TRUSTED_SOURCES =
      Set.of(
          "reuters",
          "bloomberg",
          "associated press",
          "ap news",
          "financial times",
          "the wall street journal",
          "wsj",
          "cnbc",
          "marketwatch",
          "investing.com",
          "yahoo finance");

  public List<MarketNewsArticle> select(
      List<FinnhubNewsItem> generalNews, List<FinnhubNewsItem> forexNews, Instant asOf) {
    if (asOf == null) {
      throw new IllegalArgumentException("뉴스 선별 기준 시각은 필수입니다.");
    }

    List<ScoredArticle> candidates = new ArrayList<>();
    addCandidates(candidates, generalNews, "general", asOf);
    addCandidates(candidates, forexNews, "forex", asOf);
    List<ScoredArticle> deduplicated = deduplicate(candidates);
    deduplicated.sort(
        Comparator.comparingInt(ScoredArticle::score)
            .reversed()
            .thenComparing(ScoredArticle::publishedAt, Comparator.reverseOrder())
            .thenComparing(Comparator.comparingLong(ScoredArticle::id).reversed()));

    List<ScoredArticle> selected = new ArrayList<>();
    Map<String, Integer> sourceCounts = new HashMap<>();
    int selectedForexCount = 0;
    for (ScoredArticle article : deduplicated) {
      if (selected.size() >= MAX_SELECTED_NEWS_COUNT
          || selectedForexCount >= MIN_FOREX_NEWS_COUNT) {
        break;
      }
      if ("forex".equals(article.category())
          && addIfAllowed(selected, sourceCounts, article)) {
        selectedForexCount++;
      }
    }
    for (ScoredArticle article : deduplicated) {
      if (selected.size() >= MAX_SELECTED_NEWS_COUNT) {
        break;
      }
      addIfAllowed(selected, sourceCounts, article);
    }

    selected.sort(
        Comparator.comparingInt(ScoredArticle::score)
            .reversed()
            .thenComparing(ScoredArticle::publishedAt, Comparator.reverseOrder()));
    return selected.stream().map(ScoredArticle::toArticle).toList();
  }

  private void addCandidates(
      List<ScoredArticle> target,
      List<FinnhubNewsItem> items,
      String requestedCategory,
      Instant asOf) {
    if (items == null) {
      return;
    }
    Instant oldestAllowed = asOf.minus(MAX_NEWS_AGE);
    Instant newestAllowed = asOf.plus(FUTURE_CLOCK_TOLERANCE);
    for (FinnhubNewsItem item : items) {
      if (item == null || !StringUtils.hasText(item.headline()) || !isHttpUrl(item.url())) {
        continue;
      }
      Instant publishedAt = Instant.ofEpochSecond(item.datetime());
      if (publishedAt.isBefore(oldestAllowed) || publishedAt.isAfter(newestAllowed)) {
        continue;
      }
      String category =
          StringUtils.hasText(item.category())
              ? item.category().toLowerCase(Locale.ROOT)
              : requestedCategory;
      String source = safeText(item.source(), "출처 미상", 100);
      String headline = safeText(item.headline(), "", 500);
      String summary = safeText(item.summary(), "요약 없음", 1200);
      target.add(
          new ScoredArticle(
              item.id(),
              category,
              publishedAt,
              headline,
              summary,
              source,
              item.url().trim(),
              score(item, category, source, publishedAt, asOf)));
    }
  }

  private int score(
      FinnhubNewsItem item,
      String category,
      String source,
      Instant publishedAt,
      Instant asOf) {
    long ageHours = Math.max(0, Duration.between(publishedAt, asOf).toHours());
    int score = ageHours < 6 ? 40 : ageHours < 12 ? 30 : ageHours < 24 ? 20 : 10;
    String searchable =
        (" " + item.headline() + " " + safeText(item.summary(), "", 2000) + " ")
            .toLowerCase(Locale.ROOT);
    for (Map.Entry<String, Integer> keyword : KEYWORD_WEIGHTS.entrySet()) {
      if (searchable.contains(keyword.getKey())) {
        score += keyword.getValue();
      }
    }
    if (TRUSTED_SOURCES.contains(source.toLowerCase(Locale.ROOT))) {
      score += 15;
    }
    if ("forex".equals(category)) {
      score += 8;
    }
    if (StringUtils.hasText(item.related())) {
      score += 3;
    }
    return score;
  }

  private List<ScoredArticle> deduplicate(List<ScoredArticle> candidates) {
    Map<String, ScoredArticle> byUrl = new LinkedHashMap<>();
    Set<String> headlines = new HashSet<>();
    candidates.stream()
        .sorted(Comparator.comparingInt(ScoredArticle::score).reversed())
        .forEach(
            article -> {
              String normalizedHeadline = normalizeHeadline(article.headline());
              if (headlines.add(normalizedHeadline)) {
                byUrl.putIfAbsent(article.url(), article);
              }
            });
    return new ArrayList<>(byUrl.values());
  }

  private boolean addIfAllowed(
      List<ScoredArticle> selected,
      Map<String, Integer> sourceCounts,
      ScoredArticle article) {
    if (selected.contains(article)) {
      return false;
    }
    String sourceKey = article.source().toLowerCase(Locale.ROOT);
    if (sourceCounts.getOrDefault(sourceKey, 0) >= MAX_NEWS_PER_SOURCE) {
      return false;
    }
    selected.add(article);
    sourceCounts.merge(sourceKey, 1, Integer::sum);
    return true;
  }

  private String normalizeHeadline(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
  }

  private String safeText(String value, String fallback, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }

  private boolean isHttpUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    try {
      URI uri = URI.create(value.trim());
      return uri.getHost() != null
          && ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()));
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static Map<String, Integer> keywordWeights() {
    Map<String, Integer> weights = new LinkedHashMap<>();
    List.of(
            "federal reserve",
            " fed ",
            "interest rate",
            "inflation",
            "consumer price",
            "cpi",
            "pce",
            "treasury yield",
            "bond yield",
            "jobs report",
            "unemployment",
            "central bank",
            "tariff",
            "trade war")
        .forEach(keyword -> weights.put(keyword, 14));
    List.of(
            "south korea",
            "korea",
            "kospi",
            "kosdaq",
            "korean won",
            "usd/krw",
            "krw",
            "bank of korea")
        .forEach(keyword -> weights.put(keyword, 18));
    List.of(
            "dollar",
            "currency",
            "forex",
            "semiconductor",
            "chip",
            "oil",
            "crude",
            "geopolitical",
            "stock market",
            "wall street")
        .forEach(keyword -> weights.put(keyword, 8));
    return Map.copyOf(weights);
  }

  private record ScoredArticle(
      long id,
      String category,
      Instant publishedAt,
      String headline,
      String summary,
      String source,
      String url,
      int score) {

    private MarketNewsArticle toArticle() {
      return new MarketNewsArticle(id, category, publishedAt, headline, summary, source, url);
    }
  }
}
