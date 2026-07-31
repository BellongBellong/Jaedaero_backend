package com.jaedaero.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SoldierProfileResponse {

  private final SoldierType soldierType;
  private final String rankName;

  @JsonFormat(pattern = "yyyy.MM.dd")
  private final LocalDate enlistmentDate;

  @JsonFormat(pattern = "yyyy.MM.dd")
  private final LocalDate dischargeDate;

  private final boolean savingJoinYn;
}
