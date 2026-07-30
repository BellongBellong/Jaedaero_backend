package com.jaedaero.domain.auth.vo;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthUserVo {

  private long userId;
  private String socialId;
  private String nickname;
  private ProfileImage profileImage;
  private ProfileSource profileSource;
  private boolean onboardingCompleted;

}
