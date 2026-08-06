package com.jaedaero.domain.aianalysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.aianalysis.mapper.SpendingPatternMapper;
import com.jaedaero.domain.aianalysis.vo.RecurringPaymentAggregateVo;
import com.jaedaero.domain.aianalysis.vo.SpendingCategoryAggregateVo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DbSpendingPatternProviderTest {

  @Test
  void usesMonthToDateComparisonAndNormalizesLegacyCategoryNames() {
    CapturingMapper mapper = new CapturingMapper();
    DbSpendingPatternProvider provider = new DbSpendingPatternProvider(mapper);

    SpendingPatternSnapshot result = provider.load(1L, LocalDate.of(2026, 8, 7));

    assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), mapper.currentStart);
    assertEquals(LocalDateTime.of(2026, 8, 8, 0, 0), mapper.currentEndExclusive);
    assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), mapper.previousStart);
    assertEquals(LocalDateTime.of(2026, 7, 8, 0, 0), mapper.previousEndExclusive);
    assertEquals(2, result.categories().size());
    SpendingCategoryMetric food =
        result.categories().stream().filter(metric -> "FOOD".equals(metric.category())).findFirst().orElseThrow();
    SpendingCategoryMetric asset =
        result.categories().stream().filter(metric -> "ASSET".equals(metric.category())).findFirst().orElseThrow();
    assertEquals(150_000L, food.currentAmount());
    assertEquals(550_000L, asset.currentAmount());
    assertEquals("넷플릭스", result.recurringPayments().get(0).description());
  }

  private static class CapturingMapper implements SpendingPatternMapper {
    private LocalDateTime currentStart;
    private LocalDateTime currentEndExclusive;
    private LocalDateTime previousStart;
    private LocalDateTime previousEndExclusive;

    @Override
    public List<SpendingCategoryAggregateVo> aggregateByCategory(
        long userId,
        LocalDateTime currentStart,
        LocalDateTime currentEndExclusive,
        LocalDateTime previousStart,
        LocalDateTime previousEndExclusive) {
      this.currentStart = currentStart;
      this.currentEndExclusive = currentEndExclusive;
      this.previousStart = previousStart;
      this.previousEndExclusive = previousEndExclusive;
      return List.of(
          category("FOOD", 100_000L, 80_000L, 2, 2),
          category("식비", 50_000L, 20_000L, 1, 1),
          category("저축", 550_000L, 550_000L, 1, 1));
    }

    @Override
    public List<RecurringPaymentAggregateVo> findRecurringPayments(
        long userId,
        LocalDateTime currentStart,
        LocalDateTime currentEndExclusive,
        LocalDateTime previousStart,
        LocalDateTime previousEndExclusive) {
      RecurringPaymentAggregateVo row = new RecurringPaymentAggregateVo();
      row.setDescription("넷플릭스");
      row.setCurrentAmount(17_000L);
      row.setPreviousAmount(17_000L);
      row.setCurrentTransactionCount(1);
      row.setPreviousTransactionCount(1);
      return List.of(row);
    }

    private SpendingCategoryAggregateVo category(
        String category, long current, long previous, int currentCount, int previousCount) {
      SpendingCategoryAggregateVo row = new SpendingCategoryAggregateVo();
      row.setCategory(category);
      row.setCurrentAmount(current);
      row.setPreviousAmount(previous);
      row.setCurrentTransactionCount(currentCount);
      row.setPreviousTransactionCount(previousCount);
      return row;
    }
  }
}
