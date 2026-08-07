package com.jaedaero.domain.simulation.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationRequest {

  @Size(max = 100, message = "시나리오명은 100자 이하여야 합니다.")
  private String scenarioName;

  @NotNull(message = "월 소비액은 필수입니다.")
  @PositiveOrZero(message = "월 소비액은 0 이상이어야 합니다.")
  private Long monthlySpendingAmount;

  @NotNull(message = "장병내일준비적금 월 납입액은 필수입니다.")
  @PositiveOrZero(message = "장병내일준비적금 월 납입액은 0 이상이어야 합니다.")
  @Max(value = 550000, message = "장병내일준비적금 월 납입액은 550000원 이하여야 합니다.")
  private Long monthlySavingAmount;

  @NotNull(message = "월 투자금액은 필수입니다.")
  @PositiveOrZero(message = "월 투자금액은 0 이상이어야 합니다.")
  private Long monthlyInvestmentAmount;

  @NotNull(message = "기대 수익률은 필수입니다.")
  @DecimalMin(value = "0.00", message = "기대 수익률은 0 이상이어야 합니다.")
  private BigDecimal expectedReturnRate;

  private Boolean isSaved;
}
