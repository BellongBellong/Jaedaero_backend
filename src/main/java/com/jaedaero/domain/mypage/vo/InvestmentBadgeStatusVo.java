package com.jaedaero.domain.mypage.vo;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentBadgeStatusVo {

  private InvestmentPreference initialPreference;
  private InvestmentPreference badgeTier;
  private String safeGrade;
  private String aggressiveGrade;
  private int safeMissionCount;
  private int aggressiveMissionCount;
}
