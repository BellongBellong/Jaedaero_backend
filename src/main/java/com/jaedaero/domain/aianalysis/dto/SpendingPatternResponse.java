package com.jaedaero.domain.aianalysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SpendingPatternResponse {
  private final LocalDate periodStart;
  private final LocalDate periodEnd;
  private final LocalDate comparisonPeriodStart;
  private final LocalDate comparisonPeriodEnd;
  private final Long totalSpendingAmount;
  private final Long previousTotalSpendingAmount;
  private final Long changeAmount;
  private final BigDecimal changeRate;
  private final List<SpendingCategoryResponse> categories;
}
