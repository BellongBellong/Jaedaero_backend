package com.jaedaero.domain.mypage.dto;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MyPageProfileResponse {

  private final String nickname;
  private final ProfileImage profileImage;
  private final ProfileSource profileSource;
  private final SoldierType soldierType;
  private final String militaryRank;
  private final BadgeSummaryResponse badgeSummary;
  private final InvestmentBadgeStatusResponse investmentBadgeStatus;
}
