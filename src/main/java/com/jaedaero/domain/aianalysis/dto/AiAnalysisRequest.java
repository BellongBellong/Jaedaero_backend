package com.jaedaero.domain.aianalysis.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAnalysisRequest {
  private Long simulationId;
  private Long monthlySpendingAmount;
  private Long monthlySavingAmount;
  private Long monthlyInvestmentAmount;
  private BigDecimal expectedReturnRate;
}
