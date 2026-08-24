package com.jaedaero.domain.codef.persistence;

/** 규칙으로 분류하지 못해 AI 후처리가 필요한 출금 거래의 최소 입력입니다. */
public record StoredTransactionCategoryCandidate(long transactionId, String description) {}
