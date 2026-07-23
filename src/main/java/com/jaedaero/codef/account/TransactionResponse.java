package com.jaedaero.codef.account;

import com.jaedaero.codef.persistence.StoredTransaction;
import java.time.LocalDateTime;

public class TransactionResponse {
    private final long transactionId;
    private final LocalDateTime transactionAt;
    private final long amount;
    private final Long balanceAfter;
    private final String transactionType;
    private final String category;
    private final String description;

    public TransactionResponse(StoredTransaction transaction) {
        this.transactionId = transaction.transactionId();
        this.transactionAt = transaction.transactionAt();
        this.amount = transaction.amount();
        this.balanceAfter = transaction.balanceAfter();
        this.transactionType = transaction.transactionType();
        this.category = transaction.category();
        this.description = transaction.description();
    }
    public long getTransactionId() { return transactionId; }
    public LocalDateTime getTransactionAt() { return transactionAt; }
    public long getAmount() { return amount; }
    public Long getBalanceAfter() { return balanceAfter; }
    public String getTransactionType() { return transactionType; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
}
