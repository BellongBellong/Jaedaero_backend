package com.jaedaero.domain.recurringinvestment.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class RecurringInvestmentPlanException extends BusinessException {

  public RecurringInvestmentPlanException(
      RecurringInvestmentPlanErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
