package com.jaedaero.domain.leavemode.vo;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveModeVo {

  private long leaveModeId;
  private long userId;
  private String eventName;
  private LocalDate startDate;
  private LocalDate endDate;
  private boolean leaveModeEnabled;
  private Long budgetAmount;
}
