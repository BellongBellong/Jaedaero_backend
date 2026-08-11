package com.jaedaero.domain.marketreport.service;

import java.time.Instant;
import java.util.List;

public interface MarketNewsProvider {
  List<MarketNewsArticle> fetchImportantNews(Instant asOf);
}
