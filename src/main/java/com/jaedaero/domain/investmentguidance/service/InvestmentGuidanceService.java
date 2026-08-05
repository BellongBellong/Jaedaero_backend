package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceApplyRequest;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;

public interface InvestmentGuidanceService {

  InvestmentGuidanceResponse getLatest(long userId);

  InvestmentGuidanceResponse create(long userId);

  StrategyApplicationResponse apply(
      long userId, long guidanceId, InvestmentGuidanceApplyRequest request);
}
