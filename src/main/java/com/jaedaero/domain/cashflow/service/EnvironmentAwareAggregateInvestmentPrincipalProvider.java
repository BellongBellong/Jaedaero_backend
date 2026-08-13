package com.jaedaero.domain.cashflow.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** 로컬에서는 목 제공자를, 다른 환경에서는 CODEF 제공자를 선택합니다. */
@Primary
@Component
public class EnvironmentAwareAggregateInvestmentPrincipalProvider
    implements AggregateInvestmentPrincipalProvider {

  private final AggregateInvestmentPrincipalProvider localProvider;
  private final AggregateInvestmentPrincipalProvider codefProvider;
  private final String appEnvironment;

  public EnvironmentAwareAggregateInvestmentPrincipalProvider(
      @Qualifier("localAggregateInvestmentPrincipalProvider")
          AggregateInvestmentPrincipalProvider localProvider,
      @Qualifier("codefAggregateInvestmentPrincipalProvider")
          AggregateInvestmentPrincipalProvider codefProvider,
      @Value("${app.environment:production}") String appEnvironment) {
    this.localProvider = localProvider;
    this.codefProvider = codefProvider;
    this.appEnvironment = appEnvironment;
  }

  @Override
  public Optional<Long> resolveLinkedPrincipal(long userId) {
    return isLocal()
        ? localProvider.resolveLinkedPrincipal(userId)
        : codefProvider.resolveLinkedPrincipal(userId);
  }

  private boolean isLocal() {
    return "local".equalsIgnoreCase(appEnvironment);
  }
}
