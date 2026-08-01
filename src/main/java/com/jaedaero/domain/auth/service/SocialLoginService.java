package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.dto.LoginRequest;
import com.jaedaero.domain.auth.dto.LoginResponse;
import com.jaedaero.domain.auth.dto.RefreshTokenRequest;
import com.jaedaero.domain.auth.dto.RefreshTokenResponse;

public interface SocialLoginService {

  /** 소셜 로그인 후 사용자와 토큰 정보를 반환합니다. */
  LoginResponse login(LoginRequest request);

  /** 리프레시 토큰으로 액세스 토큰을 재발급합니다. */
  RefreshTokenResponse refresh(RefreshTokenRequest request);

  /** 사용자의 리프레시 토큰을 삭제해 로그아웃 처리합니다. */
  void logout(long userId);
}
