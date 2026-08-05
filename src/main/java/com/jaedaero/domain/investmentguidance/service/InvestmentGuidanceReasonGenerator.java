package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;

public interface InvestmentGuidanceReasonGenerator {

  String generate(
      InvestmentGuidanceAction action,
      long currentContributionAmount,
      long recommendedContributionAmount,
      long expectedAsset,
      long targetAmount,
      String dataIssue);
}
