package com.jaedaero.domain.aianalysis.service;

public record SpendingCategoryMetric(
    String category,
    String displayName,
    long currentAmount,
    long previousAmount,
    int currentTransactionCount,
    int previousTransactionCount) {}
