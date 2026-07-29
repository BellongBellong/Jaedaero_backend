package com.jaedaero.codef.common;

/** Raised before a CODEF request when the requested local user does not exist. */
public class CodefUserNotFoundException extends RuntimeException {
    public CodefUserNotFoundException(long userId) {
        super("사용자를 찾을 수 없습니다. 먼저 users 테이블에 userId=" + userId + " 사용자를 생성하세요.");
    }
}
