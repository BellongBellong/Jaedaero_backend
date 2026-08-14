package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CashflowForecastResponse {
  private final Long forecastId;
  private final Long baseAsset;
  private final Long expectedSalary;
  private final Long expectedSpending;
  private final Long expectedSavingAmount;
  private final Long expectedInvestmentAmount;
  private final Long expectedAsset;
  private final Long soldierSavingPrincipal;
  private final Long soldierSavingInterest;
  private final Long governmentMatchingSupport;
  private final Long investmentPrincipal;
  private final Long expectedInvestmentReturn;
  private final Long projectedBenefitAmount;
  private final String calculationPolicyVersion;
  private final Long monthlySpendingLimit;
  private final BigDecimal achievementRate;
  private final LocalDate financialDischargeDate;
  private final List<CashflowForecastMonthResponse> months;

  public static CashflowForecastResponse from(
      CashflowForecastVo forecast, List<CashflowForecastMonthResponse> months) {
    return CashflowForecastResponse.builder()
        .forecastId(forecast.getForecastId())
        .baseAsset(forecast.getBaseAsset())
        .expectedSalary(forecast.getExpectedSalary())
        .expectedSpending(forecast.getExpectedSpending())
        .expectedSavingAmount(forecast.getExpectedSavingAmount())
        .expectedInvestmentAmount(forecast.getExpectedInvestmentAmount())
        .expectedAsset(forecast.getExpectedAsset())
        .soldierSavingPrincipal(forecast.getSoldierSavingPrincipal())
        .soldierSavingInterest(forecast.getSoldierSavingInterest())
        .governmentMatchingSupport(forecast.getGovernmentMatchingSupport())
        .investmentPrincipal(forecast.getInvestmentPrincipal())
        .expectedInvestmentReturn(forecast.getExpectedInvestmentReturn())
        .projectedBenefitAmount(forecast.getProjectedBenefitAmount())
        .calculationPolicyVersion(forecast.getCalculationPolicyVersion())
        .monthlySpendingLimit(forecast.getMonthlySpendingLimit())
        .achievementRate(forecast.getAchievementRate())
        .financialDischargeDate(forecast.getFinancialDischargeDate())
        .months(months)
        .build();
  }
}
