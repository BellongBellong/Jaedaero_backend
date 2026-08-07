package com.jaedaero.domain.simulation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.vo.SimulationInputSourceVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MockDbSimulationInputProviderTest {

  @Test
  void latestAppliedStrategyIsMappedToSimulationInput() {
    SimulationInputSourceVo source = readySource();
    source.setActiveStrategyApplicationId(7L);
    source.setAppliedMonthlySpendingAmount(120_000L);
    source.setAppliedMonthlySavingAmount(550_000L);
    source.setAppliedMonthlyInvestmentAmount(80_000L);
    source.setAppliedExpectedReturnRate(new BigDecimal("6.50"));
    MockDbSimulationInputProvider provider = new MockDbSimulationInputProvider(userId -> source);

    SimulationInput input = provider.load(1L);

    assertEquals(7L, input.appliedStrategy().applicationId());
    assertEquals(120_000L, input.appliedStrategy().monthlySpendingAmount());
    assertEquals(550_000L, input.appliedStrategy().monthlySavingAmount());
    assertEquals(80_000L, input.appliedStrategy().monthlyInvestmentAmount());
    assertEquals(new BigDecimal("6.50"), input.appliedStrategy().expectedReturnRate());
  }

  @Test
  void incompleteAppliedStrategyIsRejected() {
    SimulationInputSourceVo source = readySource();
    source.setActiveStrategyApplicationId(7L);
    source.setAppliedMonthlySpendingAmount(120_000L);
    MockDbSimulationInputProvider provider = new MockDbSimulationInputProvider(userId -> source);

    SimulationException exception =
        assertThrows(SimulationException.class, () -> provider.load(1L));

    assertEquals(SimulationErrorCode.INPUT_NOT_READY, exception.getErrorCode());
  }

  private SimulationInputSourceVo readySource() {
    SimulationInputSourceVo source = new SimulationInputSourceVo();
    source.setBaseAsset(4_300_000L);
    source.setTargetAmount(20_000_000L);
    source.setMonthlySpendingAverage(400_000L);
    source.setSoldierType("ARMY");
    source.setEnlistmentDate(LocalDate.of(2026, 3, 1));
    source.setDischargeDate(LocalDate.of(2027, 9, 1));
    return source;
  }
}
