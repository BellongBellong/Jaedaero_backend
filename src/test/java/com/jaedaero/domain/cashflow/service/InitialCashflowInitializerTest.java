package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.jaedaero.domain.auth.event.OnboardingCompletedEvent;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import org.junit.jupiter.api.Test;

class InitialCashflowInitializerTest {

  @Test
  void generatesOnlyWhenOnboardingUserHasNoForecast() {
    RecordingCashflowService cashflowService = new RecordingCashflowService(true);

    new InitialCashflowInitializer(cashflowService)
        .initializeAfterOnboarding(new OnboardingCompletedEvent(1L));

    assertEquals(1, cashflowService.generateCount);
  }

  @Test
  void keepsExistingForecastWithoutRegenerating() {
    RecordingCashflowService cashflowService = new RecordingCashflowService(false);

    new InitialCashflowInitializer(cashflowService)
        .initializeAfterOnboarding(new OnboardingCompletedEvent(1L));

    assertEquals(0, cashflowService.generateCount);
  }

  @Test
  void doesNotFailCompletedOnboardingWhenInitialGenerationIsNotReady() {
    RecordingCashflowService cashflowService = new RecordingCashflowService(true);
    cashflowService.failGeneration = true;

    assertDoesNotThrow(
        () ->
            new InitialCashflowInitializer(cashflowService)
                .initializeAfterOnboarding(new OnboardingCompletedEvent(1L)));
    assertEquals(1, cashflowService.generateCount);
  }

  private static class RecordingCashflowService implements CashflowService {

    @Override
    public com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse getCalculationInput(
        long userId) {
      throw new UnsupportedOperationException();
    }

    private final boolean missing;
    private int generateCount;
    private boolean failGeneration;

    private RecordingCashflowService(boolean missing) {
      this.missing = missing;
    }

    @Override
    public CashflowForecastResponse generate(long userId) {
      generateCount++;
      if (failGeneration) {
        throw new CashflowException(CashflowErrorCode.INPUT_NOT_READY, "입력이 아직 없습니다.");
      }
      return null;
    }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      if (missing) {
        throw new CashflowException(CashflowErrorCode.NOT_FOUND, "예측이 없습니다.");
      }
      return null;
    }

    @Override
    public CashflowForecastResponse getLatest(long userId, int months) {
      return getLatest(userId);
    }
  }
}
