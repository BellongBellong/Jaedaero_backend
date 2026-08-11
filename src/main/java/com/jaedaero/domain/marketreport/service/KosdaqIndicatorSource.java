package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.FinancialMarketIndexClient;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import com.jaedaero.domain.marketreport.model.MarketIndex;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class KosdaqIndicatorSource implements MarketIndicatorSource {

  private static final int MAX_LOOKBACK_DAYS = 10;
  private static final String INDEX_NAME = "코스닥";
  private static final DateTimeFormatter BASE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final FinancialMarketIndexClient marketIndexClient;

  public KosdaqIndicatorSource(FinancialMarketIndexClient marketIndexClient) {
    this.marketIndexClient = marketIndexClient;
  }

  @Override
  public MarketIndicatorType type() {
    return MarketIndicatorType.KOSDAQ;
  }

  @Override
  public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
    for (int daysBefore = 1; daysBefore <= MAX_LOOKBACK_DAYS; daysBefore++) {
      LocalDate candidate = businessDate.minusDays(daysBefore);
      List<MarketIndex> rows =
          marketIndexClient.getStockMarketIndex(
              candidate.format(BASE_DATE_FORMAT), INDEX_NAME);
      if (!rows.isEmpty()) {
        MarketIndex row = rows.get(0);
        return Optional.of(
            new MarketIndicatorObservation(
                type(),
                row.baseDate(),
                "금융위원회 지수시세정보",
                row.closingPrice(),
                row.change(),
                row.changeRate()));
      }
    }
    return Optional.empty();
  }
}
