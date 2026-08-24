package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.EximbankExchangeRateClient;
import com.jaedaero.domain.marketreport.client.EximbankExchangeRateItem;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UsdKrwIndicatorSource implements MarketIndicatorSource {

  private static final int INITIAL_LOOKBACK_DAYS = 1;
  private static final int MAX_LOOKBACK_DAYS = 10;
  private static final DateTimeFormatter SEARCH_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String USD_CUR_UNIT = "USD";

  private final EximbankExchangeRateClient eximbankExchangeRateClient;

  public UsdKrwIndicatorSource(EximbankExchangeRateClient eximbankExchangeRateClient) {
    this.eximbankExchangeRateClient = eximbankExchangeRateClient;
  }

  @Override
  public MarketIndicatorType type() {
    return MarketIndicatorType.USD_KRW;
  }

  @Override
  public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
    for (int offset = INITIAL_LOOKBACK_DAYS; offset <= MAX_LOOKBACK_DAYS; offset++) {
      LocalDate candidate = businessDate.minusDays(offset);
      List<EximbankExchangeRateItem> rates =
          eximbankExchangeRateClient.getExchangeRates(candidate.format(SEARCH_DATE_FORMAT));
      Optional<EximbankExchangeRateItem> usd =
          rates.stream()
              .filter(item -> item.result() == 1 && USD_CUR_UNIT.equals(item.curUnit()))
              .findFirst();
      if (usd.isPresent()) {
        BigDecimal dealBasR = new BigDecimal(usd.get().dealBasR().replace(",", ""));
        return Optional.of(
            new MarketIndicatorObservation(
                type(),
                candidate,
                "한국수출입은행 Open API",
                dealBasR,
                null,
                null));
      }
    }
    return Optional.empty();
  }
}
