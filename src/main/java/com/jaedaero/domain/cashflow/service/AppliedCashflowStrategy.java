package com.jaedaero.domain.cashflow.service;

import java.math.BigDecimal;

/** 캐시플로우 계산에 사용하는 최신 AI 추천 전략의 월 배분 스냅샷이다. */
public record AppliedCashflowStrategy(
    long applicationId,
    long monthlySpendingAmount,
    long monthlySavingAmount,
    long monthlyInvestmentAmount,
    BigDecimal expectedReturnRate) {}
