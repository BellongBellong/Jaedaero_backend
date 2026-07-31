package com.jaedaero.domain.auth.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED"),
  INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME"),
  INVALID_PROFILE_APPEARANCE(HttpStatus.BAD_REQUEST, "INVALID_PROFILE_APPEARANCE"),
  REQUIRED_AGREEMENT_NOT_ACCEPTED(HttpStatus.BAD_REQUEST, "REQUIRED_AGREEMENT_NOT_ACCEPTED"),
  INVALID_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "INVALID_SOCIAL_TYPE"),
  NICKNAME_ALREADY_IN_USE(HttpStatus.CONFLICT, "NICKNAME_ALREADY_IN_USE"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"),
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
