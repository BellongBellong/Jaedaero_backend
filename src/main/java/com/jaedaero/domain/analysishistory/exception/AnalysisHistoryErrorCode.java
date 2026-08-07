package com.jaedaero.domain.analysishistory.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AnalysisHistoryErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "ANALYSIS_HISTORY_UNAUTHENTICATED");

  private final HttpStatus status;
  private final String code;

  AnalysisHistoryErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
