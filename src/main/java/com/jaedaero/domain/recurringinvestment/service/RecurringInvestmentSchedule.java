package com.jaedaero.domain.recurringinvestment.service;

import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class RecurringInvestmentSchedule {

  private static final BigDecimal WEEKS_PER_YEAR = BigDecimal.valueOf(52);
  private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

  private RecurringInvestmentSchedule() {}

  public static long monthlyEquivalent(InvestmentFrequency frequency, long contributionAmount) {
    if (frequency == InvestmentFrequency.MONTHLY) {
      return contributionAmount;
    }
    return BigDecimal.valueOf(contributionAmount)
        .multiply(WEEKS_PER_YEAR)
        .divide(MONTHS_PER_YEAR, 0, RoundingMode.CEILING)
        .longValueExact();
  }

  public static long maximumContributionPerCycle(
      InvestmentFrequency frequency, long maxMonthlyAmount) {
    if (frequency == InvestmentFrequency.MONTHLY) {
      return maxMonthlyAmount;
    }
    return BigDecimal.valueOf(maxMonthlyAmount)
        .multiply(MONTHS_PER_YEAR)
        .divide(WEEKS_PER_YEAR, 0, RoundingMode.FLOOR)
        .longValueExact();
  }

  public static LocalDate nextDate(
      LocalDate baseDate, InvestmentFrequency frequency, int contributionDay) {
    if (frequency == InvestmentFrequency.WEEKLY) {
      DayOfWeek dayOfWeek = DayOfWeek.of(contributionDay);
      LocalDate candidate = baseDate.with(TemporalAdjusters.nextOrSame(dayOfWeek));
      return candidate.isAfter(baseDate) ? candidate : candidate.plusWeeks(1);
    }
    LocalDate candidate = baseDate.withDayOfMonth(contributionDay);
    return candidate.isAfter(baseDate)
        ? candidate
        : baseDate.plusMonths(1).withDayOfMonth(contributionDay);
  }

  public static List<LocalDate> datesUntil(
      LocalDate baseDate,
      LocalDate endDate,
      InvestmentFrequency frequency,
      int contributionDay) {
    List<LocalDate> dates = new ArrayList<>();
    LocalDate date = nextDate(baseDate, frequency, contributionDay);
    while (!date.isAfter(endDate)) {
      dates.add(date);
      date =
          frequency == InvestmentFrequency.WEEKLY ? date.plusWeeks(1) : date.plusMonths(1);
    }
    return List.copyOf(dates);
  }
}
