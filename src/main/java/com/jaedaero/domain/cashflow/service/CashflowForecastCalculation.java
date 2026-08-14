package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;
import java.util.List;

/** 순수 월별 현금흐름 계산이 반환하는 결과입니다. */
public record CashflowForecastCalculation(
    long expectedSalary,
    long expectedSpending,
    long expectedSavingAmount,
    long expectedInvestmentAmount,
    long expectedAsset,
    long soldierSavingPrincipal,
    long soldierSavingInterest,
    long governmentMatchingSupport,
    long investmentPrincipal,
    long expectedInvestmentReturn,
    long projectedBenefitAmount,
    String calculationPolicyVersion,
    long monthlySpendingLimit,
    double achievementRate,
    LocalDate financialDischargeDate,
    List<CashflowForecastMonthCalculation> months) {}
