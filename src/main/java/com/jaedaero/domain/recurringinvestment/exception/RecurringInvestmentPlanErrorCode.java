package com.jaedaero.domain.recurringinvestment.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RecurringInvestmentPlanErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "RECURRING_INVESTMENT_PLAN_UNAUTHENTICATED"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "RECURRING_INVESTMENT_PLAN_NOT_FOUND"),
  INVALID_PLAN(HttpStatus.BAD_REQUEST, "RECURRING_INVESTMENT_PLAN_INVALID"),
  INVALID_ACCOUNT(HttpStatus.BAD_REQUEST, "RECURRING_INVESTMENT_PLAN_INVALID_ACCOUNT");

  private final HttpStatus status;
  private final String code;

  RecurringInvestmentPlanErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
