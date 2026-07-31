package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;

public interface ProfileAppearanceService {

  void updateProfileAppearance(long userId, ProfileImage profileImage, ProfileSource profileSource);
}
