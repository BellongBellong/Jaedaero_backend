package com.jaedaero.domain.aianalysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jaedaero.domain.aianalysis.dto.SpendingInsightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpendingPatternAnalyzerTest {
  private final SpendingPatternAnalyzer analyzer = new SpendingPatternAnalyzer();

  @Test
  void calculatesCategoryComparisonInsightsAndExpectedEffectFromTransactions() {
    SpendingPatternSnapshot snapshot =
        new SpendingPatternSnapshot(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 7),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 7),
            List.of(
                new SpendingCategoryMetric("FOOD", "식비", 82_400L, 62_400L, 4, 3),
                new SpendingCategoryMetric("LEISURE", "여가", 35_300L, 35_300L, 1, 1),
                new SpendingCategoryMetric("ASSET", "자산", 550_000L, 550_000L, 1, 1)),
            List.of(new RecurringPaymentMetric("넷플릭스", 17_000L, 17_000L, 1, 1)));

    SpendingAnalysis result =
        analyzer.analyze(snapshot, 18_150_000L, LocalDate.of(2027, 9, 1));

    assertEquals(117_700L, result.pattern().getTotalSpendingAmount());
    assertEquals(97_700L, result.pattern().getPreviousTotalSpendingAmount());
    assertEquals(20_000L, result.pattern().getChangeAmount());
    assertEquals(new BigDecimal("20.47"), result.pattern().getChangeRate());
    assertEquals(2, result.pattern().getCategories().size());
    assertEquals("FOOD", result.pattern().getCategories().get(0).getCategory());
    assertEquals(new BigDecimal("70.01"), result.pattern().getCategories().get(0).getShareRate());
    assertEquals(new BigDecimal("32.05"), result.pattern().getCategories().get(0).getChangeRate());
    assertEquals(
        List.of(
            SpendingInsightType.SPENDING_INCREASE,
            SpendingInsightType.RECURRING_PAYMENT_CHECK,
            SpendingInsightType.SAVING_HABIT),
        result.insights().stream().map(insight -> insight.getType()).toList());
    assertEquals("FOOD", result.improvement().getTargetCategory());
    assertEquals(20_000L, result.improvement().getSuggestedMonthlyReductionAmount());
    assertEquals(13, result.expectedEffect().getRemainingMonths());
    assertEquals(260_000L, result.expectedEffect().getExpectedAssetIncreaseAmount());
    assertEquals(18_410_000L, result.expectedEffect().getExpectedAssetAfterImprovement());
  }

  @Test
  void returnsStableTemplateAndZeroEffectWhenThereAreNoTransactions() {
    SpendingPatternSnapshot snapshot =
        new SpendingPatternSnapshot(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 7),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 7),
            List.of(),
            List.of());

    SpendingAnalysis result =
        analyzer.analyze(snapshot, 18_150_000L, LocalDate.of(2027, 9, 1));

    assertEquals(0L, result.pattern().getTotalSpendingAmount());
    assertEquals(BigDecimal.ZERO.setScale(2), result.pattern().getChangeRate());
    assertEquals(SpendingInsightType.SPENDING_STABLE, result.insights().get(0).getType());
    assertNull(result.improvement().getTargetCategory());
    assertEquals(0L, result.expectedEffect().getExpectedAssetIncreaseAmount());
    assertEquals(18_150_000L, result.expectedEffect().getExpectedAssetAfterImprovement());
  }
}
