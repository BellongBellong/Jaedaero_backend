package com.jaedaero.domain.notification.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements ErrorCode {
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED"),
  TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE_TOKEN_NOT_FOUND"),
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND"),
  INVALID_NOTIFICATION_ID(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_ID");

  private final HttpStatus status;
  private final String code;

  NotificationErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
