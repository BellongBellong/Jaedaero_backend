package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class NicknameException extends BusinessException {

  public NicknameException(AuthErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
