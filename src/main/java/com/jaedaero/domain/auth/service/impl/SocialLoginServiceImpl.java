package com.jaedaero.domain.auth.service.impl;

import com.jaedaero.domain.auth.common.enums.SocialType;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.SocialAuthenticationException;
import com.jaedaero.domain.auth.dto.LoginRequest;
import com.jaedaero.domain.auth.dto.LoginResponse;
import com.jaedaero.domain.auth.dto.LoginUserResponse;
import com.jaedaero.domain.auth.dto.RefreshTokenRequest;
import com.jaedaero.domain.auth.dto.RefreshTokenResponse;
import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.auth.service.SocialLoginService;
import com.jaedaero.domain.auth.vo.AuthUserVo;
import com.jaedaero.global.security.JwtTokenProvider;
import com.jaedaero.global.security.Sha256Hasher;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class SocialLoginServiceImpl implements SocialLoginService {

  private final SocialProfileClient socialProfileClient;
  private final AuthUserMapper authUserMapper;
  private final JwtTokenProvider jwtTokenProvider;
  private final Sha256Hasher sha256Hasher;

  @Override
  @Transactional
  public LoginResponse login(LoginRequest request) {
    SocialType socialType = SocialType.from(request.getSocialType());
    AuthUserVo profile =
        socialProfileClient.exchangeAuthorizationCodeAndGetProfile(
            socialType, request.getAuthorizationCode(), request.getRedirectUri());
    AuthUserVo user = findOrCreateUser(socialType, profile);
    RefreshTokenResponse tokenResponse = createTokenResponse(user.getUserId());
    log.info("소셜 로그인 완료. socialType={}, userId={}", socialType, user.getUserId());
    return new LoginResponse(
        tokenResponse.getAccessToken(),
        tokenResponse.getRefreshToken(),
        tokenResponse.getExpiresIn(),
        new LoginUserResponse(
            user.getUserId(),
            user.getNickname(),
            socialType,
            user.getProfileImage(),
            user.getProfileSource(),
            user.isOnboardingCompleted()));
  }

  @Override
  @Transactional
  public RefreshTokenResponse refresh(RefreshTokenRequest request) {
    String tokenHash = sha256Hasher.hash(request.getRefreshToken());
    AuthUserVo user = authUserMapper.findActiveByRefreshTokenHash(tokenHash);
    if (user == null) {
      log.warn("리프레시 토큰 검증에 실패했습니다.");
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED, "리프레시 토큰이 유효하지 않거나 만료되었습니다.");
    }
    authUserMapper.deleteRefreshTokenByHash(tokenHash);
    RefreshTokenResponse tokenResponse = createTokenResponse(user.getUserId());
    log.info("액세스 토큰 재발급 완료. userId={}", user.getUserId());
    return tokenResponse;
  }

  @Override
  @Transactional
  public void logout(long userId) {
    authUserMapper.deleteRefreshTokensByUserId(userId);
    log.info("로그아웃 완료. userId={}", userId);
  }

  private AuthUserVo findOrCreateUser(SocialType socialType, AuthUserVo profile) {
    AuthUserVo user =
        authUserMapper.findActiveBySocialIdentity(socialType.name(), profile.getSocialId());
    if (user != null) {
      return user;
    }

    try {
      authUserMapper.insert(socialType.name(), profile.getSocialId());
    } catch (DuplicateKeyException exception) {
      user = authUserMapper.findActiveBySocialIdentity(socialType.name(), profile.getSocialId());
      if (user != null) {
        return user;
      }
    }

    user = authUserMapper.findActiveBySocialIdentity(socialType.name(), profile.getSocialId());
    if (user == null) {
      throw new SocialAuthenticationException(
          AuthErrorCode.WITHDRAWN_ACCOUNT, "탈퇴한 계정은 로그인할 수 없습니다.");
    }
    return user;
  }

  private String issueRefreshToken(long userId) {
    String refreshToken = jwtTokenProvider.createRefreshToken();
    authUserMapper.insertRefreshToken(
        userId,
        sha256Hasher.hash(refreshToken),
        Timestamp.from(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())));
    return refreshToken;
  }

  private RefreshTokenResponse createTokenResponse(long userId) {
    return new RefreshTokenResponse(
        jwtTokenProvider.createAccessToken(userId),
        issueRefreshToken(userId),
        jwtTokenProvider.getAccessTokenExpirationSeconds());
  }
}
