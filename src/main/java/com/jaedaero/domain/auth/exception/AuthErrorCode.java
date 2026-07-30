package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {
  INVALID_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "INVALID_SOCIAL_TYPE"),
  SOCIAL_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "SOCIAL_AUTHENTICATION_FAILED"),
  WITHDRAWN_ACCOUNT(HttpStatus.FORBIDDEN, "WITHDRAWN_ACCOUNT"),
  SOCIAL_PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "SOCIAL_PROVIDER_UNAVAILABLE");

  private final HttpStatus status;
  private final String code;

  AuthErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }

}
