package com.jaedaero.domain.investmentguidance.dto;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceVo;
import com.jaedaero.domain.investmentguidance.vo.ServiceStage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentGuidanceResponse {

  private Long guidanceId;
  private Long planId;
  private ServiceStage serviceStage;
  private InvestmentGuidanceAction actionType;
  private Long currentContributionAmount;
  private Long recommendedContributionAmount;
  private Long targetAmount;
  private Long continueExpectedAsset;
  private Long recommendedExpectedAsset;
  private Long goalMarginAmount;
  private Long investmentPrincipal;
  private Long marketValue;
  private Long unrealizedProfitLoss;
  private BigDecimal returnRate;
  private String reason;
  private LocalDateTime marketDataAsOf;
  private LocalDateTime nextReviewAt;
  private LocalDateTime createdAt;

  public static InvestmentGuidanceResponse from(InvestmentGuidanceVo source) {
    return builder()
        .guidanceId(source.getGuidanceId())
        .planId(source.getPlanId())
        .serviceStage(source.getServiceStage())
        .actionType(source.getActionType())
        .currentContributionAmount(source.getCurrentContributionAmount())
        .recommendedContributionAmount(source.getRecommendedContributionAmount())
        .targetAmount(source.getTargetAmount())
        .continueExpectedAsset(source.getContinueExpectedAsset())
        .recommendedExpectedAsset(source.getRecommendedExpectedAsset())
        .goalMarginAmount(source.getRecommendedExpectedAsset() - source.getTargetAmount())
        .investmentPrincipal(source.getInvestmentPrincipal())
        .marketValue(source.getMarketValue())
        .unrealizedProfitLoss(source.getUnrealizedProfitLoss())
        .returnRate(source.getReturnRate())
        .reason(source.getReason())
        .marketDataAsOf(source.getMarketDataAsOf())
        .nextReviewAt(source.getNextReviewAt())
        .createdAt(source.getCreatedAt())
        .build();
  }
}
