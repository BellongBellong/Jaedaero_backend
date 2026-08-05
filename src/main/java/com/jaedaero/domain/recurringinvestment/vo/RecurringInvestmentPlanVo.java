package com.jaedaero.domain.recurringinvestment.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringInvestmentPlanVo {

  private Long planId;
  private Long userId;
  private Long brokerageAccountId;
  private InvestmentFrequency frequency;
  private Integer contributionDay;
  private Long contributionAmount;
  private Long maximumMonthlyAmount;
  private String investmentProductCode;
  private String investmentProductName;
  private RecurringInvestmentPlanStatus status;
  private LocalDate nextContributionDate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
