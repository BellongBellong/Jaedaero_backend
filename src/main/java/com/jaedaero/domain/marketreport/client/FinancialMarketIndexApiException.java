package com.jaedaero.domain.marketreport.client;

/** 금융위원회 지수시세정보 API 호출 또는 응답 검증 실패. */
public class FinancialMarketIndexApiException extends RuntimeException {

  public FinancialMarketIndexApiException(String message) {
    super(message);
  }

  public FinancialMarketIndexApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
