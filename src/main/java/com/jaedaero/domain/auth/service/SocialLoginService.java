package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.dto.LoginRequest;
import com.jaedaero.domain.auth.dto.LoginResponse;
import com.jaedaero.domain.auth.dto.RefreshTokenRequest;
import com.jaedaero.domain.auth.dto.RefreshTokenResponse;

public interface SocialLoginService {

  LoginResponse login(LoginRequest request);

  RefreshTokenResponse refresh(RefreshTokenRequest request);

  void logout(long userId);
}
