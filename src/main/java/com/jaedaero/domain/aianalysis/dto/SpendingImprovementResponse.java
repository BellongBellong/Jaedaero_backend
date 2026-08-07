package com.jaedaero.domain.aianalysis.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SpendingImprovementResponse {
  private final String targetCategory;
  private final Long suggestedMonthlyReductionAmount;
  private final String action;
}
