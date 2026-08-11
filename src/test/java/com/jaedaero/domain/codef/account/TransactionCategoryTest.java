package com.jaedaero.domain.codef.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransactionCategoryTest {

  @Test
  void categorizesMerchantNamesIncludedInTransactionDescription() {
    assertEquals(TransactionCategory.PX, TransactionCategory.fromDescription("체크카드 · 국군복지단 PX"));
    assertEquals(TransactionCategory.FOOD, TransactionCategory.fromDescription("GS25 국군복지점"));
    assertEquals(TransactionCategory.FOOD, TransactionCategory.fromDescription("씨유 훈련소점"));
    assertEquals(TransactionCategory.FOOD, TransactionCategory.fromDescription("카드결제 · 스타벅스코리아"));
    assertEquals(TransactionCategory.SHOPPING, TransactionCategory.fromDescription("쿠팡(주)"));
    assertEquals(TransactionCategory.TRANSPORT, TransactionCategory.fromDescription("카카오T 택시"));
    assertEquals(TransactionCategory.LEISURE, TransactionCategory.fromDescription("넷플릭스코리아"));
    assertEquals(TransactionCategory.MEDICAL, TransactionCategory.fromDescription("OO약국"));
  }

  @Test
  void assignsEtcWhenNoMerchantRuleMatches() {
    assertEquals(TransactionCategory.ETC, TransactionCategory.fromDescription("정기결제 알 수 없음"));
  }
}
