package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;
import java.util.List;

/** 계산기 외부에서 수집한 한 건의 불변 계산 입력값입니다. */
public record CashflowInput(
    long userId,
    long baseAsset,
    long targetAmount,
    long monthlySpendingAverage,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate,
    List<SoldierSavingInput> soldierSavings,
    AppliedCashflowStrategy appliedStrategy) {

  public CashflowInput(
      long userId,
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      List<SoldierSavingInput> soldierSavings) {
    this(
        userId,
        baseAsset,
        targetAmount,
        monthlySpendingAverage,
        soldierType,
        enlistmentDate,
        dischargeDate,
        soldierSavings,
        null);
  }

  /** 사용자 문맥이 없는 순수 계산 호출의 하위 호환용 생성자입니다. */
  public CashflowInput(
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      List<SoldierSavingInput> soldierSavings,
      AppliedCashflowStrategy appliedStrategy) {
    this(
        0L,
        baseAsset,
        targetAmount,
        monthlySpendingAverage,
        soldierType,
        enlistmentDate,
        dischargeDate,
        soldierSavings,
        appliedStrategy);
  }

  /** 사용자 문맥이 없는 순수 계산 호출의 하위 호환용 생성자입니다. */
  public CashflowInput(
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      List<SoldierSavingInput> soldierSavings) {
    this(
        0L,
        baseAsset,
        targetAmount,
        monthlySpendingAverage,
        soldierType,
        enlistmentDate,
        dischargeDate,
        soldierSavings,
        null);
  }

  /** 사용자 문맥이 없는 순수 계산 호출의 하위 호환용 생성자입니다. */
  public CashflowInput(
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate) {
    this(
        0L,
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
