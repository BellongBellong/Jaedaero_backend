package com.jaedaero.codef.demo;

/** One product or stock item rendered by the securities demonstration pages. */
public class CodefDemoSecuritiesHolding {
    private final String productType;
    private final String itemName;
    private final String itemCode;
    private final String quantity;
    private final String purchaseAmount;
    private final String valuationAmount;
    private final String valuationProfit;
    private final String earningsRate;
    private final String currency;

    public CodefDemoSecuritiesHolding(String productType, String itemName, String itemCode,
            String quantity, String purchaseAmount, String valuationAmount, String valuationProfit,
            String earningsRate, String currency) {
        this.productType = productType;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.purchaseAmount = purchaseAmount;
        this.valuationAmount = valuationAmount;
        this.valuationProfit = valuationProfit;
        this.earningsRate = earningsRate;
        this.currency = currency;
    }

    public String getProductType() { return productType; }
    public String getItemName() { return itemName; }
    public String getItemCode() { return itemCode; }
    public String getQuantity() { return quantity; }
    public String getPurchaseAmount() { return purchaseAmount; }
    public String getValuationAmount() { return valuationAmount; }
    public String getValuationProfit() { return valuationProfit; }
    public String getEarningsRate() { return earningsRate; }
    public String getCurrency() { return currency; }
}
