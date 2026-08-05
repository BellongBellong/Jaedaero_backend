package com.jaedaero.domain.investmentguidance.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class InvestmentGuidanceException extends BusinessException {

  public InvestmentGuidanceException(
      InvestmentGuidanceErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
