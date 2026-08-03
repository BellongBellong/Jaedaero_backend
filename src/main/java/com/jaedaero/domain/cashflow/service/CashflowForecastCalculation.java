package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;
import java.util.List;

/** Result returned by the pure monthly cashflow calculation. */
public record CashflowForecastCalculation(
    long expectedSalary,
    long expectedSavingAmount,
    long expectedAsset,
    long monthlySpendingLimit,
    double achievementRate,
    LocalDate financialDischargeDate,
    List<CashflowForecastMonthCalculation> months) {}
