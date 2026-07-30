package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class SocialAuthenticationException extends BusinessException {

  public SocialAuthenticationException(AuthErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public SocialAuthenticationException(AuthErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
