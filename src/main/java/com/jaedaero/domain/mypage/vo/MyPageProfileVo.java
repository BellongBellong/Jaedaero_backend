package com.jaedaero.domain.mypage.vo;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPageProfileVo {

  private String nickname;
  private ProfileImage profileImage;
  private ProfileSource profileSource;
  private String militaryRank;
}
