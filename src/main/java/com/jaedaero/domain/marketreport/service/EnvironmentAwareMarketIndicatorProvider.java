package com.jaedaero.domain.marketreport.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Selects the mock indicator provider locally and the real KRX/수출입은행/FRED collector otherwise. */
@Primary
@Component
public class EnvironmentAwareMarketIndicatorProvider implements MarketIndicatorProvider {

  private final MarketIndicatorProvider mockProvider;
  private final MarketIndicatorProvider realProvider;
  private final String appEnvironment;

  public EnvironmentAwareMarketIndicatorProvider(
      @Qualifier("mockMarketIndicatorProvider") MarketIndicatorProvider mockProvider,
      @Qualifier("marketIndicatorCollector") MarketIndicatorProvider realProvider,
      @Value("${app.environment:production}") String appEnvironment) {
    this.mockProvider = mockProvider;
    this.realProvider = realProvider;
    this.appEnvironment = appEnvironment;
  }

  @Override
  public List<MarketIndicatorResult> collect(LocalDate businessDate) {
    return isLocal() ? mockProvider.collect(businessDate) : realProvider.collect(businessDate);
  }

  private boolean isLocal() {
    return "local".equalsIgnoreCase(appEnvironment);
  }
}
