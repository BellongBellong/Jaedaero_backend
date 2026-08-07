package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import java.time.LocalDate;

/** 시뮬레이션 계산에 필요한 도메인 입력값. */
public record SimulationInput(
    long baseAsset,
    long targetAmount,
    long monthlySpendingAverage,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate,
    AppliedCashflowStrategy appliedStrategy) {

  public SimulationInput(
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate) {
    this(
        baseAsset,
        targetAmount,
        monthlySpendingAverage,
        soldierType,
        enlistmentDate,
        dischargeDate,
        null);
  }
}
