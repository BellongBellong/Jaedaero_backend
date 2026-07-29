package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.StoredTransaction;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;

@ApiModel(description = "연동 계좌의 거래내역")
public class TransactionResponse {
    @ApiModelProperty(value = "거래내역 ID", example = "100")
    private final long transactionId;
    @ApiModelProperty(value = "거래 일시", example = "2026-07-29T10:30:00")
    private final LocalDateTime transactionAt;
    @ApiModelProperty(value = "거래 금액", example = "50000")
    private final long amount;
    @ApiModelProperty(value = "거래 후 잔액", example = "1450000")
    private final Long balanceAfter;
    @ApiModelProperty(value = "거래 구분(DEPOSIT: 입금, WITHDRAW: 출금)", example = "DEPOSIT")
    private final String transactionType;
    @ApiModelProperty(value = "거래 분류", example = "입금")
    private final String category;
    @ApiModelProperty(value = "거래 설명", example = "급여 입금")
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
