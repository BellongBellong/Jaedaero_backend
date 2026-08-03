package com.jaedaero.domain.aianalysis.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAnalysisInputSourceVo {
  private Long forecastId;
  private Long snapshotId;
  private Long expectedAsset;
  private LocalDate financialDischargeDate;
  private BigDecimal achievementRate;
  private Long targetAmount;
  private LocalDate actualDischargeDate;
  private Long monthlySpendingLimit;
}
