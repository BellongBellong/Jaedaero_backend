package com.jaedaero.domain.investmentguidance.vo;

import java.math.BigDecimal;
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
public class InvestmentGuidanceVo {

  private Long guidanceId;
  private Long userId;
  private Long planId;
  private Long forecastId;
  private LocalDateTime planUpdatedAt;
  private String inputDataHash;
  private ServiceStage serviceStage;
  private InvestmentGuidanceAction actionType;
  private Long targetAmount;
  private Long currentContributionAmount;
  private Long recommendedContributionAmount;
  private Long continueExpectedAsset;
  private Long recommendedExpectedAsset;
  private Long investmentPrincipal;
  private Long marketValue;
  private Long unrealizedProfitLoss;
  private BigDecimal returnRate;
  private BigDecimal expectedReturnRate;
  private Integer remainingContributionCount;
  private Long safetyBufferAmount;
  private String reason;
  private LocalDateTime marketDataAsOf;
  private LocalDateTime nextReviewAt;
  private LocalDateTime createdAt;
}
