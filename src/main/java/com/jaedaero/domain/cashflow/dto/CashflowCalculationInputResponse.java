package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.CashflowInput;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/** 다른 도메인이 캐시플로우 계산 기준을 읽을 때 사용하는 읽기 전용 계약이다. */
@Getter
@Builder
public class CashflowCalculationInputResponse {
  private final long baseAsset;
  private final long targetAmount;
  private final long monthlySpendingAverage;
  private final SoldierType soldierType;
  private final LocalDate enlistmentDate;
  private final LocalDate dischargeDate;
  private final AppliedCashflowStrategy appliedStrategy;

  public static CashflowCalculationInputResponse from(CashflowInput input) {
    return builder()
        .baseAsset(input.baseAsset())
        .targetAmount(input.targetAmount())
        .monthlySpendingAverage(input.monthlySpendingAverage())
        .soldierType(input.soldierType())
        .enlistmentDate(input.enlistmentDate())
        .dischargeDate(input.dischargeDate())
        .appliedStrategy(input.appliedStrategy())
        .build();
  }
}
