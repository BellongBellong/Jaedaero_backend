package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.cashflow.vo.CashflowForecastMonthVo;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CashflowForecastMonthResponse {
  private final LocalDate forecastMonth;
  private final String expectedRank;
  private final Long expectedSalary;
  private final Long expectedSavingAmount;
  private final Long expectedInvestmentAmount;
  private final Long expectedSpendingAmount;
  private final Long expectedEndingAsset;

  public static CashflowForecastMonthResponse from(CashflowForecastMonthVo month) {
    return CashflowForecastMonthResponse.builder()
        .forecastMonth(month.getForecastMonth())
        .expectedRank(month.getExpectedRank())
        .expectedSalary(month.getExpectedSalary())
        .expectedSavingAmount(month.getExpectedSavingAmount())
        .expectedInvestmentAmount(month.getExpectedInvestmentAmount())
        .expectedSpendingAmount(month.getExpectedSpendingAmount())
        .expectedEndingAsset(month.getExpectedEndingAsset())
        .build();
  }
}
