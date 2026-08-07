package com.jaedaero.domain.report.dto;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@ApiModel(description = "최신 캐시플로우 기준 전역 예상 리포트")
public class DischargeReportResponse {

  @ApiModelProperty(value = "캐시플로우 예측 ID", example = "12")
  private final Long forecastId;

  @ApiModelProperty(value = "예측 계산 기준 현재 자산", example = "1250000")
  private final Long baseAsset;

  @ApiModelProperty(value = "실제 전역일까지 예상되는 총 봉급", example = "14500000")
  private final Long expectedSalary;

  @ApiModelProperty(value = "장병내일준비적금 예상 수령액", example = "18000000")
  private final Long expectedSavingAmount;

  @ApiModelProperty(value = "실제 전역일까지의 예상 자산", example = "21500000")
  private final Long expectedAsset;

  @ApiModelProperty(value = "현재 자산 대비 예상 자산 증가액", example = "20250000")
  private final Long expectedAssetGrowth;

  @ApiModelProperty(value = "현재 자산 대비 예상 자산 증가율(%)", example = "1620.00")
  private final BigDecimal expectedAssetGrowthRate;

  @ApiModelProperty(value = "목표 금액 대비 예상 자산 달성률(%)", example = "107.50")
  private final BigDecimal achievementRate;

  @ApiModelProperty(value = "재정 목표 달성 예상 일자", example = "2027-03-15")
  private final LocalDate financialDischargeDate;

  @ApiModelProperty(value = "프로필에 등록된 실제 전역일", example = "2027-06-20")
  private final LocalDate actualDischargeDate;

  @ApiModelProperty(value = "실제 전역일 대비 재정적 전역일 차이(일). 양수면 실제 전역일보다 빠름", example = "97")
  private final Long deltaDaysVsActual;

  public static DischargeReportResponse from(
      CashflowForecastResponse cashflow, DashboardResponse dashboard) {
    long baseAsset = cashflow.getBaseAsset();
    long expectedAsset = cashflow.getExpectedAsset();
    long growth = expectedAsset - baseAsset;
    return DischargeReportResponse.builder()
        .forecastId(cashflow.getForecastId())
        .baseAsset(baseAsset)
        .expectedSalary(cashflow.getExpectedSalary())
        .expectedSavingAmount(cashflow.getExpectedSavingAmount())
        .expectedAsset(expectedAsset)
        .expectedAssetGrowth(growth)
        .expectedAssetGrowthRate(growthRate(growth, baseAsset))
        .achievementRate(cashflow.getAchievementRate())
        .financialDischargeDate(dashboard.getFinancialDischargeDate())
        .actualDischargeDate(dashboard.getActualDischargeDate())
        .deltaDaysVsActual(dashboard.getDeltaDaysVsActual())
        .build();
  }

  private static BigDecimal growthRate(long growth, long baseAsset) {
    if (baseAsset <= 0) {
      return null;
    }
    return BigDecimal.valueOf(growth)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(baseAsset), 2, RoundingMode.HALF_UP);
  }
}
