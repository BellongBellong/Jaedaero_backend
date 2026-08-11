package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MilitaryProductSummaryFactoryTest {

  @Test
  void createReusesExistingPolicyConstants() {
    var summary = new MilitaryProductSummaryFactory().create();

    assertEquals(new BigDecimal("5.00"), summary.getAnnualInterestRate());
    assertEquals(
        new BigDecimal("100.00"), summary.getGovernmentMatchingRate());
    assertEquals(550_000L, summary.getMaxMonthlySavingAmount());
  }
}
