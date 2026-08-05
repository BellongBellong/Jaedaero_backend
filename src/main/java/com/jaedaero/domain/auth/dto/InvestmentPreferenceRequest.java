package com.jaedaero.domain.auth.dto;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentPreferenceRequest {

  @NotNull(message = "투자 성향은 필수입니다.")
  private InvestmentPreference investmentPreference;

  @NotNull(message = "목표 금액은 필수입니다.")
  @Positive(message = "목표 금액은 0원보다 커야 합니다.")
  private Long targetAmount;
}
