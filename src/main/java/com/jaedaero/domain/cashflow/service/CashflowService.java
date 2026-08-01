package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;

public interface CashflowService {

  CashflowForecastResponse generate(long userId);

  CashflowForecastResponse getLatest(long userId);
}
