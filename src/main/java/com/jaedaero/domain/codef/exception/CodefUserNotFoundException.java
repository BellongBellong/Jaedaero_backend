package com.jaedaero.domain.codef.exception;

/** 요청한 로컬 사용자가 없을 때 CODEF 요청 전에 발생시키는 예외입니다. */
public class CodefUserNotFoundException extends RuntimeException {
  public CodefUserNotFoundException(long userId) {
    super("사용자를 찾을 수 없습니다. 먼저 users 테이블에 userId=" + userId + " 사용자를 생성하세요.");
  }
}
