package com.jaedaero.domain.investment.exception;

/** KRX Open API 요청 중 발생한 오류입니다. */
public class KrxApiException extends RuntimeException {

  private final int statusCode;

  public KrxApiException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public KrxApiException(String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
