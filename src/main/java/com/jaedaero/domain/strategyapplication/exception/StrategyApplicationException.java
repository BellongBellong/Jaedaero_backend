package com.jaedaero.domain.strategyapplication.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class StrategyApplicationException extends BusinessException {

  public StrategyApplicationException(StrategyApplicationErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public StrategyApplicationException(
      StrategyApplicationErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
