package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;

public interface CashflowService {

  /** 시뮬레이션 등 다른 도메인이 재사용할 현재 캐시플로우 계산 기준을 조회합니다. */
  CashflowCalculationInputResponse getCalculationInput(long userId);

  /** 오늘 시점의 계좌 잔액과 누적 혜택을 통합한 현재 자산을 계산합니다. */
  long getCurrentAsset(long userId);

  CashflowForecastResponse generate(long userId);

  CashflowForecastResponse getLatest(long userId);

  CashflowForecastResponse getLatest(long userId, int months);
}
