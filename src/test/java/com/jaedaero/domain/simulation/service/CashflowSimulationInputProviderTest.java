package com.jaedaero.domain.simulation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.CashflowService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CashflowSimulationInputProviderTest {

  @Test
  void latestAppliedStrategyIsMappedToSimulationInput() {
    CashflowSimulationInputProvider provider = new CashflowSimulationInputProvider(service(readySource()));

    SimulationInput input = provider.load(1L);

    assertEquals(7L, input.appliedStrategy().applicationId());
    assertEquals(120_000L, input.appliedStrategy().monthlySpendingAmount());
    assertEquals(550_000L, input.appliedStrategy().monthlySavingAmount());
    assertEquals(80_000L, input.appliedStrategy().monthlyInvestmentAmount());
    assertEquals(new BigDecimal("6.50"), input.appliedStrategy().expectedReturnRate());
  }

  @Test
  void cashflowInputNotReadyIsConvertedToSimulationInputNotReady() {
    CashflowSimulationInputProvider provider =
        new CashflowSimulationInputProvider(failingService());

    SimulationException exception =
        assertThrows(SimulationException.class, () -> provider.load(1L));

    assertEquals(SimulationErrorCode.INPUT_NOT_READY, exception.getErrorCode());
  }

  private CashflowCalculationInputResponse readySource() {
    return CashflowCalculationInputResponse.builder()
        .baseAsset(4_300_000L)
        .targetAmount(20_000_000L)
        .monthlySpendingAverage(400_000L)
        .soldierType(SoldierType.ARMY)
        .enlistmentDate(LocalDate.of(2026, 3, 1))
        .dischargeDate(LocalDate.of(2027, 9, 1))
        .appliedStrategy(new AppliedCashflowStrategy(7L, 120_000L, 550_000L, 80_000L, new BigDecimal("6.50")))
        .build();
  }

  private CashflowService service(CashflowCalculationInputResponse source) {
    return new CashflowService() {
      @Override
      public CashflowCalculationInputResponse getCalculationInput(long userId) {
        return source;
      }

      @Override
      public long getCurrentAsset(long userId) {
        return source.getBaseAsset();
      }

      @Override
      public com.jaedaero.domain.cashflow.dto.CashflowForecastResponse generate(long userId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public com.jaedaero.domain.cashflow.dto.CashflowForecastResponse getLatest(long userId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public com.jaedaero.domain.cashflow.dto.CashflowForecastResponse getLatest(long userId, int months) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private CashflowService failingService() {
    return new CashflowService() {
      @Override
      public CashflowCalculationInputResponse getCalculationInput(long userId) {
        throw new CashflowException(CashflowErrorCode.INPUT_NOT_READY, "input not ready");
      }

      @Override
      public long getCurrentAsset(long userId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public com.jaedaero.domain.cashflow.dto.CashflowForecastResponse generate(long userId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public com.jaedaero.domain.cashflow.dto.CashflowForecastResponse getLatest(long userId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public com.jaedaero.domain.cashflow.dto.CashflowForecastResponse getLatest(long userId, int months) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
