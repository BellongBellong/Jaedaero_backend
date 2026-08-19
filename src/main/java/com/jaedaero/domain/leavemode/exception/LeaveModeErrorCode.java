package com.jaedaero.domain.leavemode.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LeaveModeErrorCode implements ErrorCode {
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED"),
  LEAVE_MODE_NOT_FOUND(HttpStatus.NOT_FOUND, "LEAVE_MODE_NOT_FOUND");

  private final HttpStatus status;
  private final String code;

  LeaveModeErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
