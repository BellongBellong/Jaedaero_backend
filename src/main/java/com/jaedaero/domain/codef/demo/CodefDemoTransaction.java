package com.jaedaero.domain.codef.demo;

/** View model for one transaction card in the local JSP demonstration. */
public class CodefDemoTransaction {

  private final long transactionId;
  private final String date;
  private final String time;
  private final String description;
  private final String amount;
  private final String balance;
  private final boolean deposit;
  private final String category;

  public CodefDemoTransaction(
      long transactionId,
      String date,
      String time,
      String description,
      String amount,
      String balance,
      boolean deposit,
      String category) {
    this.transactionId = transactionId;
    this.date = date;
    this.time = time;
    this.description = description;
    this.amount = amount;
    this.balance = balance;
    this.deposit = deposit;
    this.category = category;
  }

  public long getTransactionId() {
    return transactionId;
  }

  public String getDate() {
    return date;
  }

  public String getTime() {
    return time;
  }

  public String getDescription() {
    return description;
  }

  public String getAmount() {
    return amount;
  }

  public String getBalance() {
    return balance;
  }

  public boolean isDeposit() {
    return deposit;
  }

  public String getCategory() {
    return category;
  }
}
