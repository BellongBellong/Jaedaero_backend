package com.jaedaero.domain.aianalysis.service;

import java.time.LocalDate;
import java.util.List;

public record SpendingPatternSnapshot(
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDate comparisonPeriodStart,
    LocalDate comparisonPeriodEnd,
    List<SpendingCategoryMetric> categories,
    List<RecurringPaymentMetric> recurringPayments) {}
