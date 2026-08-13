package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.FinnhubApiException;
import com.jaedaero.domain.marketreport.client.FinnhubMarketNewsClient;
import com.jaedaero.domain.marketreport.client.FinnhubNewsItem;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinnhubMarketNewsProvider implements MarketNewsProvider {

  private final FinnhubMarketNewsClient client;
  private final ImportantMarketNewsSelector selector;

  public FinnhubMarketNewsProvider(
      FinnhubMarketNewsClient client, ImportantMarketNewsSelector selector) {
    this.client = client;
    this.selector = selector;
  }

  @Override
  public List<MarketNewsArticle> fetchImportantNews(Instant asOf) {
    List<FinnhubNewsItem> generalNews = List.of();
    List<FinnhubNewsItem> forexNews = List.of();
    FinnhubApiException generalFailure = null;
    FinnhubApiException forexFailure = null;

    try {
      generalNews = client.getMarketNews("general");
    } catch (FinnhubApiException exception) {
      generalFailure = exception;
    }

    try {
      forexNews = client.getMarketNews("forex");
    } catch (FinnhubApiException exception) {
      forexFailure = exception;
    }

    if (generalFailure != null && forexFailure != null) {
      FinnhubApiException combinedFailure =
          new FinnhubApiException(
              "Finnhub 일반·외환 시장 뉴스 조회가 모두 실패했습니다.", generalFailure);
      combinedFailure.addSuppressed(forexFailure);
      throw combinedFailure;
    }

    return selector.select(generalNews, forexNews, asOf);
  }
}
