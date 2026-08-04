package com.jaedaero.domain.mypage.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MyPageErrorCode implements ErrorCode {
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND");

  private final HttpStatus status;
  private final String code;

  MyPageErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
