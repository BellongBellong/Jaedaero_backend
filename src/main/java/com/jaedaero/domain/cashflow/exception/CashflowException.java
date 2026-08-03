package com.jaedaero.domain.cashflow.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class CashflowException extends BusinessException {

  public CashflowException(CashflowErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
