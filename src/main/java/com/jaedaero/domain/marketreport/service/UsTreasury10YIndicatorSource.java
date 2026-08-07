package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.client.FredObservation;
import com.jaedaero.domain.marketreport.client.FredObservationsResponse;
import com.jaedaero.domain.marketreport.client.FredTreasuryYieldClient;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UsTreasury10YIndicatorSource implements MarketIndicatorSource {

  private static final int OBSERVATION_LIMIT = 10;
  private static final String MISSING_VALUE = ".";

  private final FredTreasuryYieldClient fredTreasuryYieldClient;

  public UsTreasury10YIndicatorSource(
      FredTreasuryYieldClient fredTreasuryYieldClient) {
    this.fredTreasuryYieldClient = fredTreasuryYieldClient;
  }

  @Override
  public MarketIndicatorType type() {
    return MarketIndicatorType.US_TREASURY_10Y;
  }

  @Override
  public Optional<MarketIndicatorObservation> fetch(LocalDate businessDate) {
    FredObservationsResponse response =
        fredTreasuryYieldClient.getRecentObservations(OBSERVATION_LIMIT);
    List<FredObservation> observations =
        response == null || response.observations() == null
            ? List.of()
            : response.observations();
    List<FredObservation> valid =
        observations.stream()
            .filter(observation -> !MISSING_VALUE.equals(observation.value()))
            .filter(
                observation ->
                    !LocalDate.parse(observation.date()).isAfter(businessDate))
            .toList();
    if (valid.isEmpty()) {
      return Optional.empty();
    }

    BigDecimal latest = new BigDecimal(valid.get(0).value());
    BigDecimal change = null;
    BigDecimal changeRate = null;
    if (valid.size() > 1) {
      BigDecimal previous = new BigDecimal(valid.get(1).value());
      change = latest.subtract(previous);
      changeRate =
          previous.signum() == 0
              ? BigDecimal.ZERO.setScale(2)
              : change
                  .multiply(BigDecimal.valueOf(100))
                  .divide(previous, 2, RoundingMode.HALF_UP);
    }
    return Optional.of(
        new MarketIndicatorObservation(
            type(),
            LocalDate.parse(valid.get(0).date()),
            "FRED DGS10",
            latest,
            change,
            changeRate));
  }
}
