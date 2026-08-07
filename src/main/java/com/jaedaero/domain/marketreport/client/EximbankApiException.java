package com.jaedaero.domain.marketreport.client;

public class EximbankApiException extends RuntimeException {

  public EximbankApiException(String message) {
    super(message);
  }

  public EximbankApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
