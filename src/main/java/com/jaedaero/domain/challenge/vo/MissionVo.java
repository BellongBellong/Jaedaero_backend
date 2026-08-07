package com.jaedaero.domain.challenge.vo;

import com.jaedaero.domain.challenge.common.enums.MissionCategory;
import com.jaedaero.domain.challenge.common.enums.MissionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissionVo {

  private long missionId;
  private MissionCategory missionCategory;
  private MissionType missionType;
  private String title;
  private String description;
  private String actionType;
  private int displayOrder;
  private boolean completed;
}
