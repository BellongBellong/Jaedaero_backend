package com.jaedaero.domain.auth.common.enums;

import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.SocialAuthenticationException;
import java.util.Locale;

public enum SocialType {
  GOOGLE,
  KAKAO;

  public static SocialType from(String value) {
    try {
      return SocialType.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new SocialAuthenticationException(
          AuthErrorCode.INVALID_SOCIAL_TYPE, "Unsupported social_type.");
    }
  }
}
