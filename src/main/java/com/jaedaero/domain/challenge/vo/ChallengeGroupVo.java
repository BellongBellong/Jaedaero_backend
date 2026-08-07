package com.jaedaero.domain.challenge.vo;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengeGroupVo {

  private long groupId;
  private SoldierType soldierType;
  private int enlistmentYear;
  private int enlistmentMonth;
}
