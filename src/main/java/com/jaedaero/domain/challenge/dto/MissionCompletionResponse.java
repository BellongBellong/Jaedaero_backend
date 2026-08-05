package com.jaedaero.domain.challenge.dto;

import com.jaedaero.domain.challenge.common.enums.MissionType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionCompletionResponse {

  private final long missionId;
  private final MissionType missionType;
  private final LocalDateTime completedAt;
  private final int safeMissionCount;
  private final String safeGrade;
  private final int aggressiveMissionCount;
  private final String aggressiveGrade;
}
