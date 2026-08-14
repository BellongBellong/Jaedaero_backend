package com.jaedaero.domain.aianalysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConsumptionReductionPolicyTest {

  private final ConsumptionReductionPolicy policy = new ConsumptionReductionPolicy();

  @Test
  void capsReductionAtThirtyPercentAndKeepsIncomeBasedLivingExpenseFloor() {
    assertEquals(70_000L, policy.recommendReduction(250_000L, 404_900L, 1_500_000L));
  }

  @Test
  void doesNotRecommendReducingPlanAlreadyAtLivingExpenseFloor() {
    assertEquals(0L, policy.recommendReduction(150_000L, 100_000L, 1_500_000L));
    assertEquals(0L, policy.recommendReduction(100_000L, 100_000L, 750_000L));
  }

  @Test
  void roundsRecommendationDownToTenThousandWon() {
    assertEquals(20_000L, policy.recommendReduction(180_000L, 27_500L, 750_000L));
  }
}
