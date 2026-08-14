package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentAwareAggregateInvestmentPrincipalProviderTest {

  @Test
  void delegatesToLocalProviderWhenAppEnvironmentIsLocal() {
    AggregateInvestmentPrincipalProvider provider =
        new EnvironmentAwareAggregateInvestmentPrincipalProvider(
            userId -> Optional.of(500_000L), userId -> Optional.of(999_999L), "local");

    assertEquals(Optional.of(500_000L), provider.resolveLinkedPrincipal(1L));
  }

  @Test
  void delegatesToCodefProviderInNonLocalEnvironments() {
    AggregateInvestmentPrincipalProvider provider =
        new EnvironmentAwareAggregateInvestmentPrincipalProvider(
            userId -> Optional.of(500_000L), userId -> Optional.of(999_999L), "production");

    assertEquals(Optional.of(999_999L), provider.resolveLinkedPrincipal(1L));
  }

  @Test
  void emptyOptionalMeansNoLinkedAccount() {
    AggregateInvestmentPrincipalProvider provider =
        new EnvironmentAwareAggregateInvestmentPrincipalProvider(
            userId -> Optional.empty(), userId -> Optional.empty(), "production");

    assertTrue(provider.resolveLinkedPrincipal(1L).isEmpty());
  }
}
