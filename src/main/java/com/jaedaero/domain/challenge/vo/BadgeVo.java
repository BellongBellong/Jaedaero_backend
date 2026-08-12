package com.jaedaero.domain.challenge.vo;

import com.jaedaero.domain.challenge.common.enums.MissionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BadgeVo {

  private long badgeId;
  private MissionType missionType;
  private int requiredCompletionCount;
  private String grade;
}
