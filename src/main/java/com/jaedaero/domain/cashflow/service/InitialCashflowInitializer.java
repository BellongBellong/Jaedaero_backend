package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.event.OnboardingCompletedEvent;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 온보딩 완료 사용자의 기본 캐시플로우를 한 번 준비합니다. */
@Slf4j
@Component
public class InitialCashflowInitializer {

  private final CashflowService cashflowService;

  public InitialCashflowInitializer(CashflowService cashflowService) {
    this.cashflowService = cashflowService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void initializeAfterOnboarding(OnboardingCompletedEvent event) {
    try {
      cashflowService.getLatest(event.userId());
    } catch (CashflowException exception) {
      if (exception.getErrorCode() != CashflowErrorCode.NOT_FOUND) {
        log.warn("온보딩 후 초기 캐시플로우 확인 실패. userId={}, code={}", event.userId(), exception.getErrorCode());
        return;
      }
      try {
        cashflowService.generate(event.userId());
      } catch (CashflowException generateException) {
        // 계좌 연동 등 후속 입력이 늦어져도 온보딩 완료 자체를 실패시키지 않는다.
        log.warn(
            "온보딩 후 초기 캐시플로우 생성 보류. userId={}, code={}",
            event.userId(),
            generateException.getErrorCode());
      } catch (RuntimeException generateException) {
        log.error("온보딩 후 초기 캐시플로우 생성 실패. userId={}", event.userId(), generateException);
      }
    } catch (RuntimeException exception) {
      log.error("온보딩 후 초기 캐시플로우 확인 실패. userId={}", event.userId(), exception);
    }
  }
}
