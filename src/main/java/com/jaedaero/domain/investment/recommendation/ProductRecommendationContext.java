package com.jaedaero.domain.investment.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;

/** AI 분석 또는 What-if 계산에서 확정된 상품 추천 조건입니다. */
public record ProductRecommendationContext(
    Long analysisId,
    Long simulationId,
    long monthlyInvestmentBudget,
    BigDecimal expectedReturnRate,
    LocalDate financialDischargeDate) {}
