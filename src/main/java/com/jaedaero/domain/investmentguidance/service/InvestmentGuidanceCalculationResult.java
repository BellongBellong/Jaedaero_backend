package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.ServiceStage;

public record InvestmentGuidanceCalculationResult(
    ServiceStage serviceStage,
    InvestmentGuidanceAction actionType,
    long currentContributionAmount,
    long recommendedContributionAmount,
    long continueExpectedAsset,
    long recommendedExpectedAsset,
    int remainingContributionCount,
    long safetyBufferAmount) {}
