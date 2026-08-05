package com.jaedaero.domain.challenge.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChallengeErrorCode implements ErrorCode {
  AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED"),
  MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_NOT_FOUND"),
  MISSION_NOT_AVAILABLE(HttpStatus.FORBIDDEN, "MISSION_NOT_AVAILABLE"),
  MISSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "MISSION_ALREADY_COMPLETED");

  private final HttpStatus status;
  private final String code;

  ChallengeErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
