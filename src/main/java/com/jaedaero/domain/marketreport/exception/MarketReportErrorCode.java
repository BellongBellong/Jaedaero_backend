package com.jaedaero.domain.marketreport.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MarketReportErrorCode implements ErrorCode {
  NOT_FOUND(HttpStatus.NOT_FOUND, "MARKET_REPORT_NOT_FOUND");

  private final HttpStatus status;
  private final String code;

  MarketReportErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
