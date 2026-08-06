package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;
import java.util.List;

/** One calculation's immutable inputs, collected outside the calculator. */
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
