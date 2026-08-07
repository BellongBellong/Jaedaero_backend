package com.jaedaero.domain.aianalysis.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SpendingInsightResponse {
  private final SpendingInsightType type;
  private final String category;
  private final String title;
  private final String description;
  private final SpendingInsightEvidenceResponse evidence;
}
