package com.jaedaero.domain.challenge.dto;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.challenge.vo.ChallengeRankingMemberVo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeTopRankerResponse {

  private final int rankingNo;
  private final String nickname;
  private final ProfileImage profileImage;
  private final ProfileSource profileSource;
  private final int missionCompletionCount;

  public static ChallengeTopRankerResponse from(ChallengeRankingMemberVo source) {
    return builder()
        .rankingNo(source.getRankingNo())
        .nickname(source.getNickname())
        .profileImage(source.getProfileImage())
        .profileSource(source.getProfileSource())
        .missionCompletionCount(source.getMissionCompletionCount())
        .build();
  }
}
