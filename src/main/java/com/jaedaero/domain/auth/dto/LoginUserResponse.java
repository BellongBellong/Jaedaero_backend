package com.jaedaero.domain.auth.dto;

import com.jaedaero.domain.auth.common.enums.SocialType;
import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginUserResponse {

  private final long userId;
  private final String nickname;
  private final SocialType socialType;
  private final ProfileImage profileImage;
  private final ProfileSource profileSource;
  private final boolean onboardingCompleted;
}
