package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;

public interface ProfileAppearanceService {

  /** 사용자의 프로필 아이콘과 배경색을 변경합니다. */
  void updateProfileAppearance(long userId, ProfileImage profileImage, ProfileSource profileSource);
}
