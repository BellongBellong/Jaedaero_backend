package com.jaedaero.domain.challenge.vo;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.challenge.common.enums.MissionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengeRankingMemberVo {

  private int rankingNo;
  private int memberCount;
  private String nickname;
  private ProfileImage profileImage;
  private ProfileSource profileSource;
  private int missionCompletionCount;
  private MissionType highestBadgeType;
  private String highestBadgeGrade;
}
