package com.jaedaero.domain.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RefreshTokenResponse {

  private final String accessToken;
  private final String refreshToken;
  private final long expiresIn;
}
