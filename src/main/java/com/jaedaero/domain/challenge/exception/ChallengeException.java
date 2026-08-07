package com.jaedaero.domain.challenge.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class ChallengeException extends BusinessException {

  public ChallengeException(ChallengeErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
