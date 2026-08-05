package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.dto.InvestmentPreferenceRequest;
import com.jaedaero.domain.auth.dto.InvestmentPreferenceResponse;

public interface InvestmentPreferenceService {

  /** 사용자의 투자 성향과 목표 금액을 등록합니다. */
  InvestmentPreferenceResponse registerInvestmentPreference(
      long userId, InvestmentPreferenceRequest request);
}
