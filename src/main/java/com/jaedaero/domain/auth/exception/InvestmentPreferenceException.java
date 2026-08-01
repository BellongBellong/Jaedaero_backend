package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class InvestmentPreferenceException extends BusinessException {

  public InvestmentPreferenceException(AuthErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
