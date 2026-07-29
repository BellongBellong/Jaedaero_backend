package com.jaedaero.domain.codef.demo;

/** View model rendered as one account card in the JSP demonstration. */
public class CodefDemoAccountCard {

  private final long accountId;
  private final String accountDisplay;
  private final String accountName;
  private final String category;
  private final String balance;
  private final boolean transactionSupported;
  private final String transactionKind;

  public CodefDemoAccountCard(
      long accountId,
      String accountDisplay,
      String accountName,
      String category,
      String balance,
      boolean transactionSupported,
      String transactionKind) {
    this.accountId = accountId;
    this.accountDisplay = accountDisplay;
    this.accountName = accountName;
    this.category = category;
    this.balance = balance;
    this.transactionSupported = transactionSupported;
    this.transactionKind = transactionKind;
  }

  public long getAccountId() {
    return accountId;
  }

  public String getAccountDisplay() {
    return accountDisplay;
  }

  public String getAccountName() {
    return accountName;
  }

  public String getCategory() {
    return category;
  }

  public String getBalance() {
    return balance;
  }

  public boolean isTransactionSupported() {
    return transactionSupported;
  }

  public String getTransactionKind() {
    return transactionKind;
  }

  public boolean isSavingsTransaction() {
    return "INSTALLMENT_SAVINGS".equals(transactionKind);
  }
}
