package com.jaedaero.domain.notification.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class NotificationException extends BusinessException {
  public NotificationException(NotificationErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
