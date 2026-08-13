package com.jaedaero.domain.codef.institution;

/** 이 애플리케이션이 지원하는 CODEF 계좌 등록 업무 구분입니다. */
public enum CodefBusinessType {
  BANK("BK"),
  SECURITIES("ST");

  private final String code;

  CodefBusinessType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CodefBusinessType fromCode(String code) {
    for (CodefBusinessType type : values()) {
      if (type.code.equals(code)) {
        return type;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 CODEF 업무 구분입니다: " + code);
  }
}
