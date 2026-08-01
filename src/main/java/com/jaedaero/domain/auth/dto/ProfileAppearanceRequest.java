package com.jaedaero.domain.auth.dto;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileAppearanceRequest {

  @NotNull private ProfileImage profileImage;

  @NotNull private ProfileSource profileSource;
}
