package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.FinnhubMarketNewsClient;
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
    return selector.select(
        client.getMarketNews("general"), client.getMarketNews("forex"), asOf);
  }
}
