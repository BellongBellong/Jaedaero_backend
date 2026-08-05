package com.jaedaero.domain.investmentguidance.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InvestmentGuidanceErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "INVESTMENT_GUIDANCE_UNAUTHENTICATED"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "INVESTMENT_GUIDANCE_NOT_FOUND"),
  INPUT_NOT_READY(HttpStatus.CONFLICT, "INVESTMENT_GUIDANCE_INPUT_NOT_READY"),
  INVALID_APPLICATION(HttpStatus.BAD_REQUEST, "INVESTMENT_GUIDANCE_INVALID_APPLICATION");

  private final HttpStatus status;
  private final String code;

  InvestmentGuidanceErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
