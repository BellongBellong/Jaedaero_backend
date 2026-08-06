package com.jaedaero.domain.cashflow.vo;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashflowForecastMonthVo {
  private Long forecastMonthId;
  private Long forecastId;
  private LocalDate forecastMonth;
  private String expectedRank;
  private Long expectedSalary;
  private Long expectedSavingAmount;
  private Long expectedInvestmentAmount;
  private Long expectedSpendingAmount;
  private Long expectedEndingAsset;
}
