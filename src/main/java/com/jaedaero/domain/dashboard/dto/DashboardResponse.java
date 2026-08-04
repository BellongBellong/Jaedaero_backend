package com.jaedaero.domain.dashboard.dto;

import com.jaedaero.domain.cashflow.dto.CashflowForecastMonthResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {
  private final LocalDate financialDischargeDate;
  private final LocalDate actualDischargeDate;
  private final Long deltaDaysVsActual;
  private final Long currentAsset;
  private final Long expectedAsset;
  private final BigDecimal achievementRate;
  private final Long thisMonthSpending;
  private final Long thisMonthSaving;

  public static DashboardResponse from(
      CashflowForecastResponse cashflow, LocalDate actualDischargeDate, LocalDate today) {
    CashflowForecastMonthResponse currentMonth =
        cashflow.getMonths().stream()
            .filter(month -> YearMonth.from(month.getForecastMonth()).equals(YearMonth.from(today)))
            .findFirst()
            .orElse(null);
    LocalDate financialDischargeDate = cashflow.getFinancialDischargeDate();
    return DashboardResponse.builder()
        .financialDischargeDate(financialDischargeDate)
        .actualDischargeDate(actualDischargeDate)
        .deltaDaysVsActual(daysVsActual(financialDischargeDate, actualDischargeDate))
        .currentAsset(cashflow.getBaseAsset())
        .expectedAsset(cashflow.getExpectedAsset())
        .achievementRate(cashflow.getAchievementRate())
        .thisMonthSpending(currentMonth == null ? 0L : currentMonth.getExpectedSpendingAmount())
        .thisMonthSaving(currentMonth == null ? 0L : currentMonth.getExpectedSavingAmount())
        .build();
  }

  private static Long daysVsActual(LocalDate financialDischargeDate, LocalDate actualDischargeDate) {
    if (financialDischargeDate == null || actualDischargeDate == null) {
      return null;
    }
    return ChronoUnit.DAYS.between(financialDischargeDate, actualDischargeDate);
  }
}
