package com.jaedaero.domain.aianalysis.service;

import com.jaedaero.domain.aianalysis.dto.SpendingCategoryResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingExpectedEffectResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingImprovementResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingInsightEvidenceResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingInsightResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingInsightType;
import com.jaedaero.domain.aianalysis.dto.SpendingPatternResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SpendingPatternAnalyzer {

  public SpendingAnalysis analyze(
      SpendingPatternSnapshot snapshot, long baselineExpectedAsset, LocalDate actualDischargeDate) {
    List<SpendingCategoryMetric> spendingMetrics =
        snapshot.categories().stream()
            .filter(metric -> !"ASSET".equals(metric.category()))
            .filter(metric -> metric.currentAmount() > 0 || metric.previousAmount() > 0)
            .toList();
    long currentTotal = spendingMetrics.stream().mapToLong(SpendingCategoryMetric::currentAmount).sum();
    long previousTotal = spendingMetrics.stream().mapToLong(SpendingCategoryMetric::previousAmount).sum();
    List<SpendingCategoryResponse> categories =
        spendingMetrics.stream().map(metric -> category(metric, currentTotal)).toList();

    SpendingPatternResponse pattern =
        SpendingPatternResponse.builder()
            .periodStart(snapshot.periodStart())
            .periodEnd(snapshot.periodEnd())
            .comparisonPeriodStart(snapshot.comparisonPeriodStart())
            .comparisonPeriodEnd(snapshot.comparisonPeriodEnd())
            .totalSpendingAmount(currentTotal)
            .previousTotalSpendingAmount(previousTotal)
            .changeAmount(currentTotal - previousTotal)
            .changeRate(changeRate(currentTotal, previousTotal))
            .categories(categories)
            .build();

    SpendingCategoryMetric increased =
        spendingMetrics.stream()
            .filter(metric -> metric.currentAmount() > metric.previousAmount())
            .max(
                Comparator.<SpendingCategoryMetric>comparingLong(
                        metric -> metric.currentAmount() - metric.previousAmount())
                    .thenComparing(SpendingCategoryMetric::category))
            .orElse(null);
    List<SpendingInsightResponse> insights = insights(snapshot, increased, currentTotal, previousTotal);
    SpendingCategoryMetric improvementTarget = currentTotal > previousTotal ? increased : null;
    long reduction =
        improvementTarget == null
            ? 0L
            : Math.min(
                improvementTarget.currentAmount() - improvementTarget.previousAmount(),
                currentTotal - previousTotal);
    int remainingMonths = remainingMonths(snapshot.periodEnd(), actualDischargeDate);
    long expectedIncrease = Math.multiplyExact(reduction, remainingMonths);
    SpendingImprovementResponse improvement =
        SpendingImprovementResponse.builder()
            .targetCategory(improvementTarget == null ? null : improvementTarget.category())
            .suggestedMonthlyReductionAmount(reduction)
            .action(
                improvementTarget == null
                    ? "현재 소비 수준을 유지하고 반복 결제 항목을 정기적으로 확인하세요."
                    : improvementTarget.displayName()
                        + "를 전월 같은 기간 수준으로 조정하면 월 "
                        + money(reduction)
                        + "원의 추가 자산 형성 여력이 생깁니다.")
            .build();
    SpendingExpectedEffectResponse effect =
        SpendingExpectedEffectResponse.builder()
            .remainingMonths(remainingMonths)
            .expectedAssetIncreaseAmount(expectedIncrease)
            .expectedAssetAfterImprovement(Math.addExact(baselineExpectedAsset, expectedIncrease))
            .assumption("제안한 월 소비 절감액을 다음 달부터 실제 전역월까지 동일하게 유지한다고 가정한 단순 합산값입니다.")
            .build();
    return new SpendingAnalysis(pattern, List.copyOf(insights), improvement, effect);
  }

  private List<SpendingInsightResponse> insights(
      SpendingPatternSnapshot snapshot,
      SpendingCategoryMetric increased,
      long currentTotal,
      long previousTotal) {
    List<SpendingInsightResponse> insights = new ArrayList<>();
    if (increased != null) {
      insights.add(
          SpendingInsightResponse.builder()
              .type(SpendingInsightType.SPENDING_INCREASE)
              .category(increased.category())
              .title(increased.displayName() + " 증가")
              .description(
                  "전월 같은 기간보다 "
                      + money(increased.currentAmount() - increased.previousAmount())
                      + "원 증가했습니다.")
              .evidence(evidence(increased))
              .build());
    }
    snapshot.recurringPayments().stream()
        .filter(payment -> payment.currentAmount() > 0)
        .findFirst()
        .ifPresent(
            payment ->
                insights.add(
                    SpendingInsightResponse.builder()
                        .type(SpendingInsightType.RECURRING_PAYMENT_CHECK)
                        .title("반복 결제 확인 필요")
                        .description(payment.description() + " 결제가 반복되어 유지 필요성을 확인해보세요.")
                        .evidence(
                            SpendingInsightEvidenceResponse.builder()
                                .currentAmount(payment.currentAmount())
                                .previousAmount(payment.previousAmount())
                                .changeAmount(payment.currentAmount() - payment.previousAmount())
                                .changeRate(changeRate(payment.currentAmount(), payment.previousAmount()))
                                .transactionCount(payment.currentTransactionCount())
                                .previousTransactionCount(payment.previousTransactionCount())
                                .build())
                        .build()));
    snapshot.categories().stream()
        .filter(metric -> "ASSET".equals(metric.category()) && metric.currentAmount() > 0)
        .findFirst()
        .ifPresent(
            saving ->
                insights.add(
                    SpendingInsightResponse.builder()
                        .type(SpendingInsightType.SAVING_HABIT)
                        .category(saving.category())
                        .title("자산 형성 습관")
                        .description("이번 기간 자산 이체 " + money(saving.currentAmount()) + "원이 확인됐습니다.")
                        .evidence(evidence(saving))
                        .build()));
    if (insights.isEmpty()) {
      insights.add(
          SpendingInsightResponse.builder()
              .type(SpendingInsightType.SPENDING_STABLE)
              .title("소비 흐름 안정")
              .description("전월 같은 기간과 비교해 두드러진 소비 증가 항목이 없습니다.")
              .evidence(
                  SpendingInsightEvidenceResponse.builder()
                      .currentAmount(currentTotal)
                      .previousAmount(previousTotal)
                      .changeAmount(currentTotal - previousTotal)
                      .changeRate(changeRate(currentTotal, previousTotal))
                      .build())
              .build());
    }
    return insights;
  }

  private SpendingCategoryResponse category(SpendingCategoryMetric metric, long total) {
    return SpendingCategoryResponse.builder()
        .category(metric.category())
        .displayName(metric.displayName())
        .amount(metric.currentAmount())
        .previousAmount(metric.previousAmount())
        .changeAmount(metric.currentAmount() - metric.previousAmount())
        .shareRate(rate(metric.currentAmount(), total))
        .changeRate(changeRate(metric.currentAmount(), metric.previousAmount()))
        .transactionCount(metric.currentTransactionCount())
        .previousTransactionCount(metric.previousTransactionCount())
        .build();
  }

  private SpendingInsightEvidenceResponse evidence(SpendingCategoryMetric metric) {
    return SpendingInsightEvidenceResponse.builder()
        .currentAmount(metric.currentAmount())
        .previousAmount(metric.previousAmount())
        .changeAmount(metric.currentAmount() - metric.previousAmount())
        .changeRate(changeRate(metric.currentAmount(), metric.previousAmount()))
        .transactionCount(metric.currentTransactionCount())
        .previousTransactionCount(metric.previousTransactionCount())
        .build();
  }

  private BigDecimal rate(long value, long total) {
    if (total == 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return BigDecimal.valueOf(value)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  private BigDecimal changeRate(long current, long previous) {
    if (previous == 0) {
      return current == 0 ? BigDecimal.ZERO.setScale(2) : null;
    }
    return BigDecimal.valueOf(current - previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
  }

  private int remainingMonths(LocalDate asOfDate, LocalDate actualDischargeDate) {
    YearMonth first = YearMonth.from(asOfDate).plusMonths(1);
    YearMonth last = YearMonth.from(actualDischargeDate);
    return first.isAfter(last) ? 0 : Math.toIntExact(ChronoUnit.MONTHS.between(first, last) + 1);
  }

  private String money(long value) {
    return String.format(Locale.KOREA, "%,d", value);
  }
}
