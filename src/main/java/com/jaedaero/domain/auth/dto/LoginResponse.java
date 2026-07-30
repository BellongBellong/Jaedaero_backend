package com.jaedaero.domain.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginResponse {

  private final String accessToken;

  private final String refreshToken;

  private final long expiresIn;

  private final LoginUserResponse user;

}
