package com.jaedaero.domain.codef.demo;

import java.util.List;

/** View model for the securities assets and stock-holdings demonstration pages. */
public class CodefDemoSecuritiesPortfolio {
  private final String accountDisplay;
  private final String depositAmount;
  private final List<CodefDemoSecuritiesHolding> holdings;

  public CodefDemoSecuritiesPortfolio(
      String accountDisplay, String depositAmount, List<CodefDemoSecuritiesHolding> holdings) {
    this.accountDisplay = accountDisplay;
    this.depositAmount = depositAmount;
    this.holdings = List.copyOf(holdings);
  }

  public String getAccountDisplay() {
    return accountDisplay;
  }

  public String getDepositAmount() {
    return depositAmount;
  }

  public List<CodefDemoSecuritiesHolding> getHoldings() {
    return holdings;
  }
}
