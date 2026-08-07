package com.jaedaero.domain.aianalysis.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecurringPaymentAggregateVo {
  private String description;
  private Long currentAmount;
  private Long previousAmount;
  private Integer currentTransactionCount;
  private Integer previousTransactionCount;
}
