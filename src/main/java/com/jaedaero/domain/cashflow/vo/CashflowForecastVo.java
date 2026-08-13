package com.jaedaero.domain.cashflow.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class CashflowForecastVo {
  private Long forecastId;
  private Long userId;
  private Long baseAsset;
  private Long expectedSalary;
  private Long expectedSpending;
  private Long expectedSavingAmount;
  private Long expectedInvestmentAmount;
  private Long expectedAsset;
  private Long soldierSavingInterest;
  private Long governmentMatchingSupport;
  private Long expectedInvestmentReturn;
  private Long projectedBenefitAmount;
  private Long potentialExpectedAsset;
  private String calculationPolicyVersion;
  private Long monthlySpendingLimit;
  private BigDecimal achievementRate;
  private LocalDate financialDischargeDate;
  private String policyVersion;
  private LocalDateTime generatedAt;
}
