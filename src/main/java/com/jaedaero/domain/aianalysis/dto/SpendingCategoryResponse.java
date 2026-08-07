package com.jaedaero.domain.aianalysis.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SpendingCategoryResponse {
  private final String category;
  private final String displayName;
  private final Long amount;
  private final Long previousAmount;
  private final Long changeAmount;
  private final BigDecimal shareRate;
  private final BigDecimal changeRate;
  private final Integer transactionCount;
  private final Integer previousTransactionCount;
}
