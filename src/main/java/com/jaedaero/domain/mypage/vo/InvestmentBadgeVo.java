package com.jaedaero.domain.mypage.vo;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentBadgeVo {
  private String badgeName;
  private String badgeDescription;
  private InvestmentPreference missionType;
  private String grade;
  private int requiredMissionCount;
  private int missionCompletedCount;
  private boolean achieved;
  private LocalDateTime acquiredAt;
}
