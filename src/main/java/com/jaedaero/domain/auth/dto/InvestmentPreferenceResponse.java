package com.jaedaero.domain.auth.dto;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InvestmentPreferenceResponse {

  private final boolean success;
  private final InvestmentPreference preference;
  private final long targetAmount;
}
