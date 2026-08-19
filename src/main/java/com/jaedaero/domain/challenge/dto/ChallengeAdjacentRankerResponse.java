package com.jaedaero.domain.challenge.dto;

import com.jaedaero.domain.challenge.vo.ChallengeRankingMemberVo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeAdjacentRankerResponse {

  private final int rank;
  private final String nickname;
  private final int missionCount;

  public static ChallengeAdjacentRankerResponse from(ChallengeRankingMemberVo source) {
    if (source == null) {
      return null;
    }

    return builder()
        .rank(source.getRankingNo())
        .nickname(source.getNickname())
        .missionCount(source.getMissionCompletionCount())
        .build();
  }
}
