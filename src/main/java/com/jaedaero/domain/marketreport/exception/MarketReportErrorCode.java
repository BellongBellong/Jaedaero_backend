package com.jaedaero.domain.marketreport.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MarketReportErrorCode implements ErrorCode {
  NOT_FOUND(HttpStatus.NOT_FOUND, "MARKET_REPORT_NOT_FOUND"),
  INDICATOR_REFRESH_NOT_AVAILABLE(
      HttpStatus.CONFLICT, "MARKET_REPORT_INDICATOR_REFRESH_NOT_AVAILABLE"),
  ADMIN_REFRESH_FORBIDDEN(HttpStatus.FORBIDDEN, "MARKET_REPORT_ADMIN_REFRESH_FORBIDDEN");

  private final HttpStatus status;
  private final String code;

  MarketReportErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
