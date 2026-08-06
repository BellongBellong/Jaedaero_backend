package com.jaedaero.domain.challenge.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengeRankingStatisticsVo {

  private int memberCount;
  private int averageMissionCompletionCount;
  private int bottomQuarterAverageMissionCompletionCount;
  private int topTenPercentThreshold;
  private LocalDateTime lastUpdatedAt;
}
