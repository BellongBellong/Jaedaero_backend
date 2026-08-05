package com.jaedaero.domain.mypage.dto;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InvestmentBadgeResponse {
  private final String badgeName;
  private final String badgeDescription;
  private final InvestmentPreference missionType;
  private final String grade;
  private final int requiredMissionCount;
  private final int missionCompletedCount;
  private final boolean achieved;
}
