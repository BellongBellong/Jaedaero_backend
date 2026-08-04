package com.jaedaero.domain.mypage.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class MyPageException extends BusinessException {

  public MyPageException(MyPageErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
