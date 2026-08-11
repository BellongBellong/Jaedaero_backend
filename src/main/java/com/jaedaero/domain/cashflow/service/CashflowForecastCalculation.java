package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;
import java.util.List;

/** 순수 월별 현금흐름 계산이 반환하는 결과입니다. */
public record CashflowForecastCalculation(
    long expectedSalary,
    long expectedSavingAmount,
    long expectedAsset,
    long monthlySpendingLimit,
    double achievementRate,
    LocalDate financialDischargeDate,
    List<CashflowForecastMonthCalculation> months) {}
