package com.jaedaero.domain.cashflow.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 대시보드 캐시플로우와 What-if가 공유하는 보수적 순자산 계산 규칙입니다.
 *
 * <p>저축·투자 원금은 급여 안에서의 배분이므로 순자산에 다시 더하지 않습니다. 아직 확정되지 않은
 * 이자·매칭지원금·투자수익도 보수적 자산과 목표 달성일에서 제외합니다.
 */
@Component
public class ConservativeMonthlyCashflowEngine {

  public static final BigDecimal SOLDIER_SAVING_ANNUAL_INTEREST_RATE =
      new BigDecimal("5.00");
  public static final BigDecimal GOVERNMENT_MATCHING_RATE = new BigDecimal("100.00");
  public static final String CALCULATION_POLICY_VERSION = "CONSERVATIVE_CASHFLOW_V3_20260813";
  private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
  private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
  private static final MathContext RETURN_MATH_CONTEXT =
      new MathContext(20, RoundingMode.HALF_UP);

  public MonthProjection project(
      long openingAsset,
      long salary,
      long spending,
      long targetAmount,
      LocalDate calculationDate,
      LocalDate dischargeDate,
      YearMonth forecastMonth) {
    long endingAsset = Math.addExact(openingAsset, Math.subtractExact(salary, spending));
    LocalDate targetReachedDate =
        estimateTargetReachedDate(
            openingAsset,
            endingAsset,
            targetAmount,
            calculationDate,
            dischargeDate,
            forecastMonth);
    return new MonthProjection(endingAsset, targetReachedDate);
  }

  public ProjectedBenefit calculateProjectedBenefit(
      List<SoldierSavingInput> existingSoldierSavings,
      LocalDate calculationDate,
      List<Long> monthlySavingContributions,
      List<LocalDate> monthlySavingContributionDates,
      LocalDate dischargeDate,
      long existingInvestmentPrincipal,
      List<Long> monthlyInvestmentContributions,
      BigDecimal investmentAnnualReturnRate) {
    LocalDate savingMaturityDate = resolveSavingMaturityDate(existingSoldierSavings, dischargeDate);

    long existingSavingBalance = 0L;
    long existingSavingInterest = 0L;
    for (SoldierSavingInput saving : existingSoldierSavings) {
      existingSavingBalance = Math.addExact(existingSavingBalance, saving.currentBalance());
      long elapsedMonths =
          Math.max(0L, ChronoUnit.MONTHS.between(saving.startDate(), calculationDate));
      existingSavingInterest =
          Math.addExact(
              existingSavingInterest,
              simpleInterest(
                  saving.currentBalance(), SOLDIER_SAVING_ANNUAL_INTEREST_RATE, elapsedMonths));
    }

    long futureSavingInterest = 0L;
    for (int index = 0; index < monthlySavingContributions.size(); index++) {
      long amount = monthlySavingContributions.get(index);
      long remainingMonths =
          Math.max(
              0L,
              ChronoUnit.MONTHS.between(
                  monthlySavingContributionDates.get(index), savingMaturityDate));
      futureSavingInterest =
          Math.addExact(
              futureSavingInterest,
              simpleInterest(amount, SOLDIER_SAVING_ANNUAL_INTEREST_RATE, remainingMonths));
    }

    long savingPrincipal = Math.addExact(existingSavingBalance, sum(monthlySavingContributions));
    long savingInterest = Math.addExact(existingSavingInterest, futureSavingInterest);
    long governmentMatchingSupport = rateAmount(savingPrincipal, GOVERNMENT_MATCHING_RATE);

    long investmentPrincipal =
        Math.addExact(existingInvestmentPrincipal, sum(monthlyInvestmentContributions));
    long investmentReturn =
        compoundReturn(
            existingInvestmentPrincipal, monthlyInvestmentContributions, investmentAnnualReturnRate);

    long projectedBenefitAmount =
        Math.addExact(Math.addExact(savingInterest, governmentMatchingSupport), investmentReturn);
    return new ProjectedBenefit(
        savingPrincipal,
        savingInterest,
        governmentMatchingSupport,
        investmentPrincipal,
        investmentReturn,
        projectedBenefitAmount);
  }

  private LocalDate resolveSavingMaturityDate(
      List<SoldierSavingInput> existingSoldierSavings, LocalDate dischargeDate) {
    return existingSoldierSavings.stream()
        .map(SoldierSavingInput::maturityDate)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(dischargeDate);
  }

  private long simpleInterest(long principal, BigDecimal annualRatePercent, long months) {
    if (principal == 0L || months <= 0L) {
      return 0L;
    }
    return BigDecimal.valueOf(principal)
        .multiply(annualRatePercent)
        .multiply(BigDecimal.valueOf(months))
        .divide(PERCENT.multiply(MONTHS_PER_YEAR), 0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private long sum(List<Long> contributions) {
    long total = 0L;
    for (Long contribution : contributions) {
      total = Math.addExact(total, contribution == null ? 0L : contribution);
    }
    return total;
  }

  private long compoundReturn(
      long existingPrincipal, List<Long> contributions, BigDecimal annualRatePercent) {
    if (annualRatePercent == null || annualRatePercent.signum() == 0) {
      return 0L;
    }
    BigDecimal monthlyRate =
        annualRatePercent
            .divide(PERCENT, RETURN_MATH_CONTEXT)
            .divide(MONTHS_PER_YEAR, RETURN_MATH_CONTEXT);
    BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate);
    BigDecimal balance = BigDecimal.valueOf(existingPrincipal);
    for (Long contribution : contributions) {
      balance =
          balance
              .multiply(growthFactor, RETURN_MATH_CONTEXT)
              .add(BigDecimal.valueOf(contribution == null ? 0L : contribution));
    }
    long totalPrincipal = Math.addExact(existingPrincipal, sum(contributions));
    return balance
        .subtract(BigDecimal.valueOf(totalPrincipal))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private long rateAmount(long amount, BigDecimal ratePercent) {
    return BigDecimal.valueOf(amount)
        .multiply(ratePercent)
        .divide(PERCENT, 0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private LocalDate estimateTargetReachedDate(
      long openingAsset,
      long endingAsset,
      long targetAmount,
      LocalDate calculationDate,
      LocalDate dischargeDate,
      YearMonth forecastMonth) {
    if (openingAsset >= targetAmount || endingAsset < targetAmount) {
      return null;
    }
    long monthlyNetIncrease = endingAsset - openingAsset;
    if (monthlyNetIncrease <= 0L) {
      return null;
    }

    LocalDate periodStart =
        forecastMonth.equals(YearMonth.from(calculationDate))
            ? calculationDate
            : forecastMonth.atDay(1);
    LocalDate periodEnd =
        forecastMonth.equals(YearMonth.from(dischargeDate))
            ? dischargeDate
            : forecastMonth.atEndOfMonth();
    int periodDays = (int) ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
    long amountNeeded = targetAmount - openingAsset;
    long daysToReach =
        (Math.multiplyExact(amountNeeded, periodDays) + monthlyNetIncrease - 1)
            / monthlyNetIncrease;
    return periodStart.plusDays(daysToReach - 1);
  }

  public record MonthProjection(long endingAsset, LocalDate targetReachedDate) {}

  public record ProjectedBenefit(
      long soldierSavingPrincipal,
      long soldierSavingInterest,
      long governmentMatchingSupport,
      long investmentPrincipal,
      long expectedInvestmentReturn,
      long projectedBenefitAmount) {}
}
