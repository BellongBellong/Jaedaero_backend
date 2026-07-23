package com.jaedaero.codef.persistence;

import java.time.LocalDateTime;

public record StoredTransaction(long transactionId, LocalDateTime transactionAt, long amount,
        Long balanceAfter, String transactionType, String category, String description) {}
