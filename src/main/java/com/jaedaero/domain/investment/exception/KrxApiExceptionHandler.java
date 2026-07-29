package com.jaedaero.domain.investment.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jaedaero.domain.investment")
public class KrxApiExceptionHandler {

  @ExceptionHandler(KrxApiException.class)
  public ResponseEntity<Map<String, String>> handleKrxApiException(KrxApiException exception) {
    HttpStatus status = HttpStatus.resolve(exception.getStatusCode());
    Map<String, String> response = new LinkedHashMap<>();
    response.put("code", "KRX_REQUEST_FAILED");
    response.put("message", exception.getMessage());
    return ResponseEntity.status(status == null ? HttpStatus.BAD_GATEWAY : status).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleInvalidRequest(
      IllegalArgumentException exception) {
    Map<String, String> response = new LinkedHashMap<>();
    response.put("code", "INVALID_REQUEST");
    response.put("message", exception.getMessage());
    return ResponseEntity.badRequest().body(response);
  }
}
