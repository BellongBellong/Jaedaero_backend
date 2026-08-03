package com.jaedaero.domain.cashflow.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CashflowErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "CASHFLOW_UNAUTHENTICATED"),
  INPUT_NOT_READY(HttpStatus.CONFLICT, "CASHFLOW_INPUT_NOT_READY"),
  PAY_POLICY_NOT_FOUND(HttpStatus.CONFLICT, "CASHFLOW_PAY_POLICY_NOT_FOUND"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "CASHFLOW_NOT_FOUND");

  private final HttpStatus status;
  private final String code;

  CashflowErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
