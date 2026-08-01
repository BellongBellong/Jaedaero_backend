package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class AgreementException extends BusinessException {

  public AgreementException(AuthErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
