package com.jaedaero.domain.marketreport.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MilitaryProductSummaryItem {
  private final BigDecimal annualInterestRate;
  private final BigDecimal governmentMatchingRate;
  private final Long maxMonthlySavingAmount;
}
