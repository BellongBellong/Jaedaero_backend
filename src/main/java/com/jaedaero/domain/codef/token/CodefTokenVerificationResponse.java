package com.jaedaero.domain.codef.token;

/** 개발용 토큰 검증에 안전한 응답입니다. 액세스 토큰은 절대 노출하지 않습니다. */
public class CodefTokenVerificationResponse {

  private final boolean issued;
  private final String tokenType;
  private final Long expiresIn;
  private final String scope;

  private CodefTokenVerificationResponse(
      boolean issued, String tokenType, Long expiresIn, String scope) {
    this.issued = issued;
    this.tokenType = tokenType;
    this.expiresIn = expiresIn;
    this.scope = scope;
  }

  public static CodefTokenVerificationResponse from(CodefTokenResponse tokenResponse) {
    return new CodefTokenVerificationResponse(
        true, tokenResponse.getTokenType(), tokenResponse.getExpiresIn(), tokenResponse.getScope());
  }

  public boolean isIssued() {
    return issued;
  }

  public String getTokenType() {
    return tokenType;
  }

  public Long getExpiresIn() {
    return expiresIn;
  }

  public String getScope() {
    return scope;
  }
}
