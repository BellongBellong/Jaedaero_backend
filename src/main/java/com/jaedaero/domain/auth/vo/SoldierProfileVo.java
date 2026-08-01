package com.jaedaero.domain.auth.vo;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoldierProfileVo {

  private long userId;
  private SoldierType soldierType;
  private String rankName;
  private LocalDate enlistmentDate;
  private LocalDate dischargeDate;
  private boolean savingJoinYn;
}
