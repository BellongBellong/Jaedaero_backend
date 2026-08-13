package com.jaedaero.domain.dashboard.dto;

import com.jaedaero.domain.cashflow.dto.CashflowForecastMonthResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  @ApiModelProperty(value = "이번 달 예상 수입", example = "1905000")
  private final Long thisMonthIncome;

  @ApiModelProperty(value = "이번 달 예상 투자액", example = "420000")
  private final Long thisMonthInvestment;

  @ApiModelProperty(value = "이번 달 실제 소비액(동기화된 출금 거래 합계)", example = "154000")
  private final Long thisMonthSpending;

  @ApiModelProperty(value = "적용한 What-if 기준 월 투자 목표액", example = "500000")
  private final Long monthlyInvestmentGoal;

  @ApiModelProperty(value = "적용한 What-if 기준 월 소비 목표액", example = "100000")
  private final Long monthlySpendingGoal;

  @ApiModelProperty(value = "월 투자 목표 달성률(%)", example = "84.00")
  private final BigDecimal investmentGoalAchievementRate;

  @ApiModelProperty(value = "월 소비 목표 대비 사용률(%)", example = "154.00")
  private final BigDecimal spendingGoalAchievementRate;

  @ApiModelProperty(value = "현재 목표의 적용 출처", example = "SIMULATION")
  private final String goalSource;

  @ApiModelProperty(value = "현재 목표 적용 일시", example = "2026-08-06T14:30:00")
  private final LocalDateTime goalAppliedAt;

  public static DashboardResponse from(
      CashflowForecastResponse cashflow,
      LocalDate actualDischargeDate,
      StrategyApplicationVo latestApplication,
      LocalDate today,
      Long actualThisMonthSpending) {
    CashflowForecastMonthResponse currentMonth =
        cashflow.getMonths().stream()
            .filter(month -> YearMonth.from(month.getForecastMonth()).equals(YearMonth.from(today)))
            .findFirst()
            .orElse(null);
    LocalDate financialDischargeDate = cashflow.getFinancialDischargeDate();
    long thisMonthIncome = currentMonth == null ? 0L : currentMonth.getExpectedSalary();
    long thisMonthInvestment =
        currentMonth == null || currentMonth.getExpectedInvestmentAmount() == null
            ? 0L
            : currentMonth.getExpectedInvestmentAmount();
    long thisMonthSpending = actualThisMonthSpending == null ? 0L : actualThisMonthSpending;
    Long monthlyInvestmentGoal = monthlyInvestmentGoal(latestApplication);
    Long monthlySpendingGoal =
        latestApplication == null ? 0L : latestApplication.getAppliedMonthlySpendingAmount();
    return DashboardResponse.builder()
        .financialDischargeDate(financialDischargeDate)
        .actualDischargeDate(actualDischargeDate)
        .deltaDaysVsActual(daysVsActual(financialDischargeDate, actualDischargeDate))
        .currentAsset(cashflow.getBaseAsset())
        .expectedAsset(cashflow.getExpectedAsset())
        .achievementRate(cashflow.getAchievementRate())
        .thisMonthIncome(thisMonthIncome)
        .thisMonthInvestment(thisMonthInvestment)
        .thisMonthSpending(thisMonthSpending)
        .monthlyInvestmentGoal(monthlyInvestmentGoal)
        .monthlySpendingGoal(monthlySpendingGoal)
        .investmentGoalAchievementRate(achievementRate(thisMonthInvestment, monthlyInvestmentGoal))
        .spendingGoalAchievementRate(achievementRate(thisMonthSpending, monthlySpendingGoal))
        .goalSource(
            latestApplication == null || latestApplication.getSourceType() == null
                ? null
                : latestApplication.getSourceType().name())
        .goalAppliedAt(latestApplication == null ? null : latestApplication.getAppliedAt())
        .build();
  }

  private static Long monthlyInvestmentGoal(StrategyApplicationVo application) {
    return application == null ? 0L : application.getAppliedMonthlyInvestmentAmount();
  }

  private static BigDecimal achievementRate(long actualAmount, Long goalAmount) {
    if (goalAmount == null || goalAmount <= 0) {
      return null;
    }
    return BigDecimal.valueOf(actualAmount)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(goalAmount), 2, RoundingMode.HALF_UP);
  }

  private static Long daysVsActual(LocalDate financialDischargeDate, LocalDate actualDischargeDate) {
    if (financialDischargeDate == null || actualDischargeDate == null) {
      return null;
    }
    return ChronoUnit.DAYS.between(financialDischargeDate, actualDischargeDate);
  }
}
