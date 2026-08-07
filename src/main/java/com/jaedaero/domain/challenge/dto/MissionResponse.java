package com.jaedaero.domain.challenge.dto;

import com.jaedaero.domain.challenge.common.enums.MissionCategory;
import com.jaedaero.domain.challenge.common.enums.MissionType;
import com.jaedaero.domain.challenge.vo.MissionVo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionResponse {

  private final long missionId;
  private final MissionCategory missionCategory;
  private final MissionType missionType;
  private final String title;
  private final String description;
  private final String actionType;
  private final boolean completed;

  public static MissionResponse from(MissionVo mission) {
    return MissionResponse.builder()
        .missionId(mission.getMissionId())
        .missionCategory(mission.getMissionCategory())
        .missionType(mission.getMissionType())
        .title(mission.getTitle())
        .description(mission.getDescription())
        .actionType(mission.getActionType())
        .completed(mission.isCompleted())
        .build();
  }
}
