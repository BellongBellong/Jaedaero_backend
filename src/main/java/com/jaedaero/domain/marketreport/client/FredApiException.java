package com.jaedaero.domain.marketreport.client;

public class FredApiException extends RuntimeException {

  public FredApiException(String message) {
    super(message);
  }

  public FredApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
