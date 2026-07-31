package com.jaedaero.domain.auth.dto;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentPreferenceRequest {

  @NotNull private InvestmentPreference investmentPreference;

  @NotNull
  @PositiveOrZero
  private Long targetAmount;
}
