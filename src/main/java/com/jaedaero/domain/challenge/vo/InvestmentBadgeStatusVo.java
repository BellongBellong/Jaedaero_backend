package com.jaedaero.domain.challenge.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentBadgeStatusVo {

  private int safeCount;
  private String safeGrade;
  private int aggressiveCount;
  private String aggressiveGrade;
}
