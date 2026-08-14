package com.jaedaero.domain.cashflow.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoldierSavingInputSourceVo {
  private Long currentBalance;
  private Long monthlyAmount;
  private BigDecimal interestRate;
  private Long governmentSupportExpected;
  private LocalDate startDate;
  private LocalDate endDate;
}
