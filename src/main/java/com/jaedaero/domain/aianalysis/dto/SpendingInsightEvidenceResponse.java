package com.jaedaero.domain.aianalysis.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SpendingInsightEvidenceResponse {
  private final Long currentAmount;
  private final Long previousAmount;
  private final Long changeAmount;
  private final BigDecimal changeRate;
  private final Integer transactionCount;
  private final Integer previousTransactionCount;
}
