package com.jaedaero.domain.challenge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeGroupResponse {

  private final long groupId;
  private final RankingPeriod rankingPeriod;
  private final String cohortName;
  private final SoldierType soldierType;
  private final int enlistmentYear;
  private final int enlistmentMonth;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private final LocalDate periodStartDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private final LocalDate periodEndDate;
  private final int memberCount;
  private final List<ChallengeTopRankerResponse> topRankers;
  private final int myRankingNo;
  private final int myPercentile;
  private final int myMissionCompletionCount;
  private final int groupAverageMissionCompletionCount;
  private final int bottomQuarterAverageMissionCompletionCount;
  private final int topTenPercentThreshold;
  private final int averageDifference;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private final LocalDateTime lastUpdatedAt;
}
