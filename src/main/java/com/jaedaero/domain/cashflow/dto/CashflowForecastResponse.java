package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CashflowForecastResponse {
  private final Long forecastId;
  private final Long baseAsset;
  private final Long expectedSalary;
  private final Long expectedSavingAmount;
  private final Long expectedAsset;
  private final Long monthlySpendingLimit;
  private final BigDecimal achievementRate;
  private final LocalDate financialDischargeDate;
  private final String policyVersion;
  private final LocalDateTime generatedAt;
  private final List<CashflowForecastMonthResponse> months;

  public static CashflowForecastResponse from(
      CashflowForecastVo forecast, List<CashflowForecastMonthResponse> months) {
    return CashflowForecastResponse.builder()
        .forecastId(forecast.getForecastId())
        .baseAsset(forecast.getBaseAsset())
        .expectedSalary(forecast.getExpectedSalary())
        .expectedSavingAmount(forecast.getExpectedSavingAmount())
        .expectedAsset(forecast.getExpectedAsset())
        .monthlySpendingLimit(forecast.getMonthlySpendingLimit())
        .achievementRate(forecast.getAchievementRate())
        .financialDischargeDate(forecast.getFinancialDischargeDate())
        .policyVersion(forecast.getPolicyVersion())
        .generatedAt(forecast.getGeneratedAt())
        .months(months)
        .build();
  }
}
