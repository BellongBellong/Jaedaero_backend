package com.jaedaero.domain.aianalysis.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpendingCategoryAggregateVo {
  private String category;
  private Long currentAmount;
  private Long previousAmount;
  private Integer currentTransactionCount;
  private Integer previousTransactionCount;
}
