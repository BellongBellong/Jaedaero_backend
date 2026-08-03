package com.jaedaero.domain.strategyapplication.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StrategyApplicationErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "STRATEGY_APPLICATION_UNAUTHENTICATED"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "STRATEGY_APPLICATION_NOT_FOUND"),
  INVALID_SOURCE(HttpStatus.CONFLICT, "STRATEGY_APPLICATION_INVALID_SOURCE");

  private final HttpStatus status;
  private final String code;

  StrategyApplicationErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
