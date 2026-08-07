package com.jaedaero.domain.aianalysis.service;

import com.jaedaero.domain.aianalysis.mapper.SpendingPatternMapper;
import com.jaedaero.domain.aianalysis.vo.RecurringPaymentAggregateVo;
import com.jaedaero.domain.aianalysis.vo.SpendingCategoryAggregateVo;
import com.jaedaero.domain.codef.account.TransactionCategory;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbSpendingPatternProvider implements SpendingPatternProvider {
  private final SpendingPatternMapper mapper;

  @Override
  public SpendingPatternSnapshot load(long userId, LocalDate asOfDate) {
    LocalDate currentStart = asOfDate.withDayOfMonth(1);
    YearMonth previousMonth = YearMonth.from(asOfDate).minusMonths(1);
    LocalDate previousStart = previousMonth.atDay(1);
    LocalDate previousEnd = previousMonth.atDay(Math.min(asOfDate.getDayOfMonth(), previousMonth.lengthOfMonth()));

    List<SpendingCategoryAggregateVo> rows =
        mapper.aggregateByCategory(
            userId,
            currentStart.atStartOfDay(),
            asOfDate.plusDays(1).atStartOfDay(),
            previousStart.atStartOfDay(),
            previousEnd.plusDays(1).atStartOfDay());
    List<RecurringPaymentAggregateVo> recurringRows =
        mapper.findRecurringPayments(
            userId,
            currentStart.atStartOfDay(),
            asOfDate.plusDays(1).atStartOfDay(),
            previousStart.atStartOfDay(),
            previousEnd.plusDays(1).atStartOfDay());

    return new SpendingPatternSnapshot(
        currentStart,
        asOfDate,
        previousStart,
        previousEnd,
        normalizeCategories(rows),
        recurringRows.stream().map(this::toRecurringMetric).toList());
  }

  private List<SpendingCategoryMetric> normalizeCategories(
      List<SpendingCategoryAggregateVo> rows) {
    Map<String, MutableMetric> merged = new LinkedHashMap<>();
    for (SpendingCategoryAggregateVo row : rows) {
      TransactionCategory category = category(row.getCategory());
      MutableMetric metric =
          merged.computeIfAbsent(
              category.name(), ignored -> new MutableMetric(category.name(), category.getDisplayName()));
      metric.currentAmount += value(row.getCurrentAmount());
      metric.previousAmount += value(row.getPreviousAmount());
      metric.currentCount += value(row.getCurrentTransactionCount());
      metric.previousCount += value(row.getPreviousTransactionCount());
    }
    List<SpendingCategoryMetric> result = new ArrayList<>();
    for (MutableMetric metric : merged.values()) {
      result.add(
          new SpendingCategoryMetric(
              metric.category,
              metric.displayName,
              metric.currentAmount,
              metric.previousAmount,
              metric.currentCount,
              metric.previousCount));
    }
    result.sort(
        Comparator.comparingLong(SpendingCategoryMetric::currentAmount)
            .reversed()
            .thenComparing(SpendingCategoryMetric::category));
    return List.copyOf(result);
  }

  private TransactionCategory category(String value) {
    if ("저축".equals(value) || "자산".equals(value)) {
      return TransactionCategory.ASSET;
    }
    try {
      return TransactionCategory.from(value);
    } catch (IllegalArgumentException exception) {
      return TransactionCategory.ETC;
    }
  }

  private RecurringPaymentMetric toRecurringMetric(RecurringPaymentAggregateVo row) {
    return new RecurringPaymentMetric(
        row.getDescription(),
        value(row.getCurrentAmount()),
        value(row.getPreviousAmount()),
        value(row.getCurrentTransactionCount()),
        value(row.getPreviousTransactionCount()));
  }

  private long value(Long value) {
    return value == null ? 0L : value;
  }

  private int value(Integer value) {
    return value == null ? 0 : value;
  }

  private static class MutableMetric {
    private final String category;
    private final String displayName;
    private long currentAmount;
    private long previousAmount;
    private int currentCount;
    private int previousCount;

    private MutableMetric(String category, String displayName) {
      this.category = category;
      this.displayName = displayName;
    }
  }
}
