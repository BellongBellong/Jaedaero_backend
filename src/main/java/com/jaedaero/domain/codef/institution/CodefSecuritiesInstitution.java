package com.jaedaero.domain.codef.institution;

/** CODEF 증권(ST) 기관 목록입니다. */
public enum CodefSecuritiesInstitution {
  YUANTA("0209", "유안타증권"),
  KB("0218", "KB증권"),
  IBK("0225", "IBK투자증권"),
  DAOL("0227", "다올투자증권"),
  MIRAe_ASSET("0238", "미래에셋증권"),
  SAMSUNG("0240", "삼성증권"),
  KOREA_INVESTMENT("0243", "한국투자증권"),
  NH("0247", "NH투자증권"),
  NH_NAMUH("1247", "NH투자증권 모바일증권 나무"),
  KYOBO("0261", "교보증권"),
  HI("0262", "하이투자증권"),
  KIWOOM("0264", "키움증권"),
  LS("0265", "LS증권"),
  SK("0266", "SK증권"),
  DAISHIN("0267", "대신증권"),
  DAISHIN_CREON("1267", "대신증권 크레온"),
  HANWHA("0269", "한화투자증권"),
  HANA("0270", "하나증권"),
  SHINHAN("0278", "신한투자증권"),
  DB("0279", "DB금융투자"),
  EUGENE("0280", "유진투자증권"),
  MERITZ("0287", "메리츠증권");

  private final String organizationCode;
  private final String displayName;

  CodefSecuritiesInstitution(String organizationCode, String displayName) {
    this.organizationCode = organizationCode;
    this.displayName = displayName;
  }

  public String getOrganizationCode() {
    return organizationCode;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static CodefSecuritiesInstitution fromOrganizationCode(String organizationCode) {
    for (CodefSecuritiesInstitution institution : values()) {
      if (institution.organizationCode.equals(organizationCode)) {
        return institution;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 CODEF 증권사 기관 코드입니다: " + organizationCode);
  }
}
