package com.jaedaero.domain.leavemode.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class LeaveModeException extends BusinessException {

  public LeaveModeException(LeaveModeErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
