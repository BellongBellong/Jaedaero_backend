package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class MilitaryInfoException extends BusinessException {

  public MilitaryInfoException(AuthErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
