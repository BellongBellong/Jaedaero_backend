package com.jaedaero.domain.codef.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CODEF OAuth 클라이언트 자격 증명 토큰 응답입니다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodefTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("expires_in")
  private Long expiresIn;

  private String scope;

  public String getAccessToken() {
    return accessToken;
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
