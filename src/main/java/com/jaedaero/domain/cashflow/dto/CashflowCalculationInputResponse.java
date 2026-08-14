package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.CashflowInput;
import com.jaedaero.domain.cashflow.service.SoldierSavingInput;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 다른 도메인이 캐시플로우 계산 기준을 읽을 때 사용하는 읽기 전용 계약이다. */
@Getter
@Builder
public class CashflowCalculationInputResponse {
  private final long userId;
  private final long baseAsset;
  private final long targetAmount;
  private final long monthlySpendingAverage;
  private final SoldierType soldierType;
  private final LocalDate enlistmentDate;
  private final LocalDate dischargeDate;
  private final List<SoldierSavingInput> soldierSavings;
  private final AppliedCashflowStrategy appliedStrategy;

  public static CashflowCalculationInputResponse from(CashflowInput input) {
    return builder()
        .userId(input.userId())
        .baseAsset(input.baseAsset())
        .targetAmount(input.targetAmount())
        .monthlySpendingAverage(input.monthlySpendingAverage())
        .soldierType(input.soldierType())
        .enlistmentDate(input.enlistmentDate())
        .dischargeDate(input.dischargeDate())
        .soldierSavings(input.soldierSavings())
        .appliedStrategy(input.appliedStrategy())
        .build();
  }
}
