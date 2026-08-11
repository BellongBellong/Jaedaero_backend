package com.jaedaero.domain.marketreport.client;

public class FinnhubApiException extends RuntimeException {

  public FinnhubApiException(String message) {
    super(message);
  }

  public FinnhubApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
