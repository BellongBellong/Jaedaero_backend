package com.jaedaero.codef.institution;

/** CODEF account-registration business categories supported by this application. */
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
