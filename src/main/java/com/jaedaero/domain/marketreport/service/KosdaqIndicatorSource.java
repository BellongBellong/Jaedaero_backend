package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.KrxIndexClient;
import com.jaedaero.domain.marketreport.client.KrxIndexDailyTradingInfo;
import com.jaedaero.domain.marketreport.client.KrxIndexDailyTradingResponse;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class KosdaqIndicatorSource implements MarketIndicatorSource {

  private static final int MAX_LOOKBACK_DAYS = 10;
  private static final DateTimeFormatter BASE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final KrxIndexClient krxIndexClient;

  public KosdaqIndicatorSource(KrxIndexClient krxIndexClient) {
    this.krxIndexClient = krxIndexClient;
  }

  @Override
  public MarketIndicatorType type() {
    return MarketIndicatorType.KOSDAQ;
  }

  @Override
  public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
    for (int offset = 0; offset <= MAX_LOOKBACK_DAYS; offset++) {
      LocalDate candidate = businessDate.minusDays(offset);
      KrxIndexDailyTradingResponse response =
          krxIndexClient.getKosdaqDailyTrading(candidate.format(BASE_DATE_FORMAT));
      List<KrxIndexDailyTradingInfo> rows =
          response == null || response.outBlock1() == null
              ? List.of()
              : response.outBlock1();
      if (!rows.isEmpty()) {
        KrxIndexDailyTradingInfo row = rows.get(0);
        return Optional.of(
            new MarketIndicatorObservation(
                type(),
                candidate,
                "KRX Open API",
                new BigDecimal(row.clsprcIdx()),
                new BigDecimal(row.cmpprevddIdx()),
                new BigDecimal(row.flucRtIdx())));
      }
    }
    return Optional.empty();
  }
}
