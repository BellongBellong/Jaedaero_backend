package com.jaedaero.domain.mypage.dto;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InvestmentBadgeStatusResponse {

  private final InvestmentPreference initialPreference;
  private final InvestmentPreference badgeTier;
  private final String safeGrade;
  private final String aggressiveGrade;
  private final int safeMissionCount;
  private final int aggressiveMissionCount;
  private final int missionCompletedCount;
}
