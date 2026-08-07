package com.jaedaero.domain.simulation.service;

import java.math.BigDecimal;

/** 월 배분금액에서 파생한 화면 표시용 지표다. */
public record SimulationAllocationMetrics(
    long referenceMonthlyIncome,
    BigDecimal spendingRate,
    BigDecimal savingRate,
    BigDecimal investmentRate,
    long unallocatedAmount) {}
