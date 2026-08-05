package com.jaedaero.domain.simulation.vo;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** MyBatis가 읽는 캐시플로우·목표·적금의 임시 결합 조회 결과다. */
@Getter
@Setter
public class SimulationInputSourceVo {

  private Long baseAsset;
  private Long targetAmount;
  private String soldierType;
  private LocalDate enlistmentDate;
  private LocalDate dischargeDate;
}
