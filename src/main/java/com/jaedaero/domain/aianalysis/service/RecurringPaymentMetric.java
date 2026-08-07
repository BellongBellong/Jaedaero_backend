package com.jaedaero.domain.aianalysis.service;

public record RecurringPaymentMetric(
    String description,
    long currentAmount,
    long previousAmount,
    int currentTransactionCount,
    int previousTransactionCount) {}
