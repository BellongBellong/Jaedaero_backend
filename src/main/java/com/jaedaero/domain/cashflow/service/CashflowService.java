package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;

public interface CashflowService {

  /** 시뮬레이션 등 다른 도메인이 재사용할 현재 캐시플로우 계산 기준을 조회합니다. */
  CashflowCalculationInputResponse getCalculationInput(long userId);

  CashflowForecastResponse generate(long userId);

  CashflowForecastResponse getLatest(long userId);

  CashflowForecastResponse getLatest(long userId, int months);
}
