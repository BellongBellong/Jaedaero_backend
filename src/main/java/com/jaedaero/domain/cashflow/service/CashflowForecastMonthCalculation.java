package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;

/** 저장되기 전 월별 계산 결과 한 건입니다. */
public record CashflowForecastMonthCalculation(
    LocalDate forecastMonth,
    String expectedRank,
    long expectedSalary,
    long expectedSavingAmount,
    long expectedInvestmentAmount,
    long expectedSpendingAmount,
    long expectedEndingAsset) {}
