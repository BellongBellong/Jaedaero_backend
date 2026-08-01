package com.jaedaero.domain.simulation.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
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

  @NotNull(message = "월 저축액은 필수입니다.")
  @PositiveOrZero(message = "월 저축액은 0 이상이어야 합니다.")
  private Long monthlySavingAmount;

  @NotNull(message = "투자 비율은 필수입니다.")
  @DecimalMin(value = "0.00", message = "투자 비율은 0 이상이어야 합니다.")
  @DecimalMax(value = "100.00", message = "투자 비율은 100 이하여야 합니다.")
  private BigDecimal investmentRatio;

  @NotNull(message = "기대 수익률은 필수입니다.")
  @DecimalMin(value = "0.00", message = "기대 수익률은 0 이상이어야 합니다.")
  private BigDecimal expectedReturnRate;

  private Boolean isSaved;
}
