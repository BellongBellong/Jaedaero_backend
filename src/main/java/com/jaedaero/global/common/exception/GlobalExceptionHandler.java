package com.jaedaero.global.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus())
        .body(new ApiErrorResponse(errorCode.getCode(), exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().isEmpty()
            ? "요청 값이 올바르지 않습니다."
            : exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
    return ResponseEntity.badRequest()
        .body(new ApiErrorResponse("INVALID_REQUEST", message));
  }
}
