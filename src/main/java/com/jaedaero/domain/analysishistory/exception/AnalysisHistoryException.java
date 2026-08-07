package com.jaedaero.domain.analysishistory.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class AnalysisHistoryException extends BusinessException {

  public AnalysisHistoryException(AnalysisHistoryErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
