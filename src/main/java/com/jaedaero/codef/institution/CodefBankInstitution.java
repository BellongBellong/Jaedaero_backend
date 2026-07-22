package com.jaedaero.codef.institution;

/**
 * CODEF bank (BK) organization catalogue used by the bank-selection UI.
 *
 * <p>The list is isolated in one enum so it can be replaced with a database-managed catalogue when
 * CODEF institution/login-support information is synchronized. An organization can support a
 * different login method over time; the frontend must display only methods confirmed in CODEF's
 * current institution guide.
 */
public enum CodefBankInstitution {
    KOREA_DEVELOPMENT_BANK("0002", "산업은행"),
    IBK("0003", "IBK기업은행"),
    KOOKMIN("0004", "KB국민은행"),
    SUHYUP("0007", "수협은행"),
    NONGHYUP("0011", "NH농협은행"),
    WOORI("0020", "우리은행"),
    STANDARD_CHARTERED("0023", "SC제일은행"),
    CITI("0027", "한국씨티은행"),
    DAEGU("0031", "iM뱅크"),
    BUSAN("0032", "부산은행"),
    GWANGJU("0034", "광주은행"),
    JEJU("0035", "제주은행"),
    JEONBUK("0037", "전북은행"),
    KYONGNAM("0039", "경남은행"),
    SAEMAUL("0045", "새마을금고"),
    CREDIT_UNION("0048", "신협"),
    KOREA_POST("0071", "우체국"),
    HANA("0081", "하나은행"),
    SHINHAN("0088", "신한은행"),
    KBANK("0089", "케이뱅크"),
    KAKAO_BANK("0000", "카카오뱅크"),
    TOSS_BANK("0001", "토스뱅크");

    private final String organizationCode;
    private final String displayName;

    CodefBankInstitution(String organizationCode, String displayName) {
        this.organizationCode = organizationCode;
        this.displayName = displayName;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CodefBankInstitution fromOrganizationCode(String organizationCode) {
        for (CodefBankInstitution institution : values()) {
            if (institution.organizationCode.equals(organizationCode)) {
                return institution;
            }
        }
        throw new IllegalArgumentException("Unsupported CODEF bank organization: " + organizationCode);
    }
}
