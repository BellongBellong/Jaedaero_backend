package com.jaedaero.domain.codef.account;

/** AI가 반환한 거래 카테고리 분류 결과입니다. */
public record TransactionCategoryClassification(
    long transactionId, TransactionCategory category) {}
