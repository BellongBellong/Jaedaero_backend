package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.SoldierSavingInput;
import java.time.LocalDate;
import java.util.List;

/** 시뮬레이션 계산에 필요한 도메인 입력값. */
public record SimulationInput(
    long userId,
    long baseAsset,
    long targetAmount,
    long monthlySpendingAverage,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate,
    List<SoldierSavingInput> soldierSavings,
    AppliedCashflowStrategy appliedStrategy) {

  public SimulationInput(
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
  public SimulationInput(
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      AppliedCashflowStrategy appliedStrategy) {
    this(
        0L,
        baseAsset,
        targetAmount,
        monthlySpendingAverage,
        soldierType,
        enlistmentDate,
        dischargeDate,
        List.of(),
        appliedStrategy);
  }

  /** 사용자 문맥이 없는 순수 계산 호출의 하위 호환용 생성자입니다. */
  public SimulationInput(
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
