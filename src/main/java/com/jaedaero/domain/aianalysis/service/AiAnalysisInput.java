package com.jaedaero.domain.aianalysis.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiAnalysisInput(
    long forecastId,
    Long snapshotId,
    long expectedAsset,
    LocalDate financialDischargeDate,
    BigDecimal achievementRate,
    long targetAmount,
    LocalDate actualDischargeDate,
    long monthlySpendingLimit,
    SpendingPatternSnapshot spendingPattern) {}
