package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class ProfileAppearanceException extends BusinessException {

  public ProfileAppearanceException(AuthErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
