package com.jaedaero.domain.recurringinvestment.dto;

import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanStatus;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringInvestmentPlanResponse {

  private Long planId;
  private Long brokerageAccountId;
  private InvestmentFrequency frequency;
  private Integer contributionDay;
  private Long contributionAmount;
  private Long monthlyEquivalentAmount;
  private Long maximumMonthlyAmount;
  private String investmentProductCode;
  private String investmentProductName;
  private RecurringInvestmentPlanStatus status;
  private LocalDate nextContributionDate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static RecurringInvestmentPlanResponse from(
      RecurringInvestmentPlanVo source, long monthlyEquivalentAmount) {
    return builder()
        .planId(source.getPlanId())
        .brokerageAccountId(source.getBrokerageAccountId())
        .frequency(source.getFrequency())
        .contributionDay(source.getContributionDay())
        .contributionAmount(source.getContributionAmount())
        .monthlyEquivalentAmount(monthlyEquivalentAmount)
        .maximumMonthlyAmount(source.getMaximumMonthlyAmount())
        .investmentProductCode(source.getInvestmentProductCode())
        .investmentProductName(source.getInvestmentProductName())
        .status(source.getStatus())
        .nextContributionDate(source.getNextContributionDate())
        .createdAt(source.getCreatedAt())
        .updatedAt(source.getUpdatedAt())
        .build();
  }
}
