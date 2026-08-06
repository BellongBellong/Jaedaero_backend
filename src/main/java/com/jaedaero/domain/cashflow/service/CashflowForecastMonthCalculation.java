package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;

/** A single monthly row before it is persisted. */
public record CashflowForecastMonthCalculation(
    LocalDate forecastMonth,
    String expectedRank,
    long expectedSalary,
    long expectedSavingAmount,
    long expectedInvestmentAmount,
    long expectedSpendingAmount,
    long expectedEndingAsset) {}
