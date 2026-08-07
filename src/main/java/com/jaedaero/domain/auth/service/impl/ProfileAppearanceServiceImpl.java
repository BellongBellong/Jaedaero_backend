package com.jaedaero.domain.auth.service.impl;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.ProfileAppearanceException;
import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.auth.service.ProfileAppearanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileAppearanceServiceImpl implements ProfileAppearanceService {

  private final AuthUserMapper authUserMapper;

  /** 사용자의 프로필 이미지와 배경을 변경합니다. */
  @Override
  @Transactional
  public void updateProfileAppearance(
      long userId, ProfileImage profileImage, ProfileSource profileSource) {
    if (profileImage == null || profileSource == null) {
      throw new ProfileAppearanceException(
          AuthErrorCode.INVALID_PROFILE_APPEARANCE, "프로필 아이콘과 배경색을 모두 선택해야 합니다.");
    }

    if (authUserMapper.updateProfileAppearance(userId, profileImage, profileSource) == 0) {
      throw new ProfileAppearanceException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }
  }
}
