package com.jaedaero.domain.aianalysis.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AiAnalysisErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "AI_ANALYSIS_UNAUTHENTICATED"),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "AI_ANALYSIS_INVALID_REQUEST"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "AI_ANALYSIS_NOT_FOUND"),
  INPUT_NOT_READY(HttpStatus.CONFLICT, "AI_ANALYSIS_INPUT_NOT_READY"),
  RESULT_UNREADABLE(HttpStatus.INTERNAL_SERVER_ERROR, "AI_ANALYSIS_RESULT_UNREADABLE");

  private final HttpStatus status;
  private final String code;
  AiAnalysisErrorCode(HttpStatus status, String code) { this.status = status; this.code = code; }
}
