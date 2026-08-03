package com.jaedaero.domain.aianalysis.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class AiAnalysisException extends BusinessException {
  public AiAnalysisException(AiAnalysisErrorCode code, String message) { super(code, message); }
  public AiAnalysisException(AiAnalysisErrorCode code, String message, Throwable cause) { super(code, message, cause); }
}
