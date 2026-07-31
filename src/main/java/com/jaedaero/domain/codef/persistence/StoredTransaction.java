package com.jaedaero.domain.codef.persistence;

import java.time.LocalDateTime;

public record StoredTransaction(
    long transactionId,
    long accountId,
    LocalDateTime transactionAt,
    long amount,
    Long balanceAfter,
    String transactionType,
    String category,
    String description) {}
