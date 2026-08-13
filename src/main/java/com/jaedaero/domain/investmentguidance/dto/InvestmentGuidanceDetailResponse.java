package com.jaedaero.domain.investmentguidance.dto;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceVo;
import com.jaedaero.domain.investmentguidance.vo.ServiceStage;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentSchedule;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanStatus;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 투자 가이드가 계산된 시점의 결과와 현재 내부 적립 계획·적용 상태를 함께 제공합니다. */
@Getter
@Builder
public class InvestmentGuidanceDetailResponse {

  private static final String NO_ORDER_NOTICE =
      "이 가이드는 다음 적립 행동 안내이며 실제 증권 주문이나 보유자산 자동 매도를 실행하지 않습니다.";

  private Long guidanceId;
  private ServiceStage serviceStage;
  private String currentRankName;
  private CurrentPlan currentPlan;
  private AssetStatus assetStatus;
  private GoalProgress goalProgress;
  private Recommendation recommendation;
  private ApplicationStatus applicationStatus;
  private String reasonGenerationPolicy;
  private String noOrderNotice;

  public static InvestmentGuidanceDetailResponse from(
      InvestmentGuidanceVo guidance,
      RecurringInvestmentPlanVo plan,
      String currentRankName,
      StrategyApplicationVo application) {
    return builder()
        .guidanceId(guidance.getGuidanceId())
        .serviceStage(guidance.getServiceStage())
        .currentRankName(currentRankName)
        .currentPlan(
            new CurrentPlan(
                plan.getPlanId(),
                plan.getBrokerageAccountId(),
                plan.getFrequency(),
                plan.getContributionDay(),
                plan.getContributionAmount(),
                RecurringInvestmentSchedule.monthlyEquivalent(
                    plan.getFrequency(), plan.getContributionAmount()),
                plan.getMaximumMonthlyAmount(),
                plan.getInvestmentProductCode(),
                plan.getInvestmentProductName(),
                plan.getStatus(),
                plan.getNextContributionDate()))
        .assetStatus(
            new AssetStatus(
                guidance.getInvestmentPrincipal(),
                guidance.getMarketValue(),
                guidance.getUnrealizedProfitLoss(),
                guidance.getReturnRate(),
                guidance.getSafeAssetAmount(),
                guidance.getRiskAssetAmount(),
                guidance.getMarketDataAsOf()))
        .goalProgress(
            new GoalProgress(
                guidance.getTargetAmount(),
                guidance.getContinueExpectedAsset(),
                guidance.getRecommendedExpectedAsset(),
                guidance.getRecommendedExpectedAsset() - guidance.getTargetAmount()))
        .recommendation(
            new Recommendation(
                guidance.getActionType(),
                guidance.getCurrentContributionAmount(),
                guidance.getRecommendedContributionAmount(),
                guidance.getExpectedReturnRate(),
                guidance.getRemainingContributionCount(),
                guidance.getSafetyBufferAmount(),
                guidance.getReason(),
                guidance.getCreatedAt(),
                guidance.getMarketDataAsOf(),
                guidance.getNextReviewAt()))
        .applicationStatus(ApplicationStatus.from(application))
        .reasonGenerationPolicy("TEMPLATE")
        .noOrderNotice(NO_ORDER_NOTICE)
        .build();
  }

  public record CurrentPlan(
      Long planId,
      Long brokerageAccountId,
      InvestmentFrequency frequency,
      Integer contributionDay,
      Long contributionAmount,
      Long monthlyEquivalentAmount,
      Long maximumMonthlyAmount,
      String investmentProductCode,
      String investmentProductName,
      RecurringInvestmentPlanStatus status,
      LocalDate nextContributionDate) {}

  public record AssetStatus(
      Long investmentPrincipal,
      Long marketValue,
      Long unrealizedProfitLoss,
      BigDecimal returnRate,
      Long safeAssetAmount,
      Long riskAssetAmount,
      LocalDateTime marketDataAsOf) {}

  public record GoalProgress(
      Long targetAmount,
      Long continueExpectedAsset,
      Long recommendedExpectedAsset,
      Long goalMarginAmount) {}

  public record Recommendation(
      InvestmentGuidanceAction actionType,
      Long currentContributionAmount,
      Long recommendedContributionAmount,
      BigDecimal expectedReturnRate,
      Integer remainingContributionCount,
      Long safetyBufferAmount,
      String reason,
      LocalDateTime calculationBasedAt,
      LocalDateTime marketDataAsOf,
      LocalDateTime nextReviewAt) {}

  public record ApplicationStatus(
      boolean applied,
      LocalDateTime appliedAt,
      InvestmentGuidanceAction appliedAction,
      Long appliedContributionAmount) {
    private static ApplicationStatus from(StrategyApplicationVo application) {
      if (application == null) {
        return new ApplicationStatus(false, null, null, null);
      }
      return new ApplicationStatus(
          true,
          application.getAppliedAt(),
          application.getAppliedGuidanceAction(),
          application.getAppliedRecurringContributionAmount());
    }
  }
}
