package com.jaedaero.domain.codef.token;

/**
 * CODEF OAuth 토큰 발급에 실패했을 때 발생합니다. 민감한 응답 데이터는 의도적으로 제외합니다.
 */
public class CodefTokenException extends RuntimeException {

  private final int statusCode;

  public CodefTokenException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public CodefTokenException(String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
