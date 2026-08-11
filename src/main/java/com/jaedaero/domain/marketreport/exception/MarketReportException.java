package com.jaedaero.domain.marketreport.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class MarketReportException extends BusinessException {
  public MarketReportException(
      MarketReportErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
