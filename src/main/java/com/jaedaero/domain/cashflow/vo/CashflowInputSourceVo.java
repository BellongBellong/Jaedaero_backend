package com.jaedaero.domain.cashflow.vo;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashflowInputSourceVo {
  private Long baseAsset;
  private Long targetAmount;
  private Long monthlySpendingAverage;
  private String soldierType;
  private LocalDate enlistmentDate;
  private LocalDate dischargeDate;
}
