package com.jaedaero.domain.dashboard.dto;

import com.jaedaero.domain.cashflow.dto.CashflowForecastMonthResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@ApiModel(description = "홈 대시보드에 표시할 캐시플로우 요약 정보")
public class DashboardResponse {
  @ApiModelProperty(value = "재정 목표를 달성하는 예상 일자", example = "2027-03-15")
  private final LocalDate financialDischargeDate;

  @ApiModelProperty(value = "프로필에 등록된 실제 전역일", example = "2027-06-20")
  private final LocalDate actualDischargeDate;

  @ApiModelProperty(value = "실제 전역일 대비 재정적 전역일 차이(일). 양수면 실제 전역일보다 빠름", example = "97")
  private final Long deltaDaysVsActual;

  @ApiModelProperty(value = "계산 기준 현재 자산", example = "1250000")
  private final Long currentAsset;

  @ApiModelProperty(value = "실제 전역일까지의 예상 자산", example = "18250000")
  private final Long expectedAsset;

  @ApiModelProperty(value = "목표 금액 대비 예상 자산 달성률(%)", example = "91.25")
  private final BigDecimal achievementRate;

  @ApiModelProperty(value = "이번 달 예상 소비액", example = "0")
  private final Long thisMonthSpending;

  @ApiModelProperty(value = "이번 달 예상 저축액", example = "550000")
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
