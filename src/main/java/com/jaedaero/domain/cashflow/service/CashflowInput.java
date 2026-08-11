package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;
import java.util.List;

/** 계산기 외부에서 수집한 한 건의 불변 계산 입력값입니다. */
public record CashflowInput(
    long baseAsset,
    long targetAmount,
    long monthlySpendingAverage,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate,
    List<SoldierSavingInput> soldierSavings,
    AppliedCashflowStrategy appliedStrategy) {

  public CashflowInput(
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      List<SoldierSavingInput> soldierSavings) {
    this(
        baseAsset,
        targetAmount,
        monthlySpendingAverage,
        soldierType,
        enlistmentDate,
        dischargeDate,
        soldierSavings,
        null);
  }

  public CashflowInput(
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
        List.of(),
        null);
  }
}
