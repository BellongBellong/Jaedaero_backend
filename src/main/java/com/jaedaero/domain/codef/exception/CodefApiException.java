package com.jaedaero.domain.codef.exception;

/** CODEF 리소스 API 호출 실패 예외입니다. */
public class CodefApiException extends RuntimeException {

  private final int statusCode;

  public CodefApiException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public CodefApiException(String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
