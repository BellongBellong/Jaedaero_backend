package com.jaedaero.domain.aianalysis.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SpendingExpectedEffectResponse {
  private final Integer remainingMonths;
  private final Long expectedAssetIncreaseAmount;
  private final Long expectedAssetAfterImprovement;
  private final String assumption;
}
