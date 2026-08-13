package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 월별 급여, 저축·투자 배분, 지출 및 기말 자산 예측을 계산합니다. */
@Component
public class CashflowCalculator {

  /** 동기화된 장병 적금 금액을 가져오기 전까지 사용할 초기 배분값입니다. */
  public static final long DEFAULT_MONTHLY_SAVING_AMOUNT = 550_000L;

  private final DefaultMilitaryPayPolicy militaryPayPolicy;
  private final ConservativeMonthlyCashflowEngine cashflowEngine;
  private final AggregateInvestmentPrincipalProvider investmentPrincipalProvider;
  private final InvestmentPrincipalBackfill investmentPrincipalBackfill;

  @Autowired
  public CashflowCalculator(
      DefaultMilitaryPayPolicy militaryPayPolicy,
      ConservativeMonthlyCashflowEngine cashflowEngine,
      AggregateInvestmentPrincipalProvider investmentPrincipalProvider,
      InvestmentPrincipalBackfill investmentPrincipalBackfill) {
    this.militaryPayPolicy = militaryPayPolicy;
    this.cashflowEngine = cashflowEngine;
    this.investmentPrincipalProvider = investmentPrincipalProvider;
    this.investmentPrincipalBackfill = investmentPrincipalBackfill;
  }

  /** 단위 테스트와 독립 계산 호출의 하위 호환용 생성자입니다. */
  public CashflowCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this(
        militaryPayPolicy,
        new ConservativeMonthlyCashflowEngine(),
        userId -> java.util.Optional.empty(),
        new InvestmentPrincipalBackfill(militaryPayPolicy));
  }

  public CashflowForecastCalculation calculate(CashflowInput input, LocalDate calculationDate) {
    YearMonth startMonth = YearMonth.from(calculationDate);
    YearMonth dischargeMonth = YearMonth.from(input.dischargeDate());
    if (calculationDate.isAfter(input.dischargeDate())) {
      return new CashflowForecastCalculation(
          0L,
          0L,
          0L,
          0L,
          input.baseAsset(),
          0L,
          0L,
          0L,
          0L,
          0L,
          0L,
          ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION,
          0L,
          achievementRate(input.baseAsset(), input.targetAmount()),
          null,
          List.of());
    }

    long asset = input.baseAsset();
    long expectedSalary = 0L;
    long expectedSpending = 0L;
    long expectedSavingAmount = 0L;
    long expectedInvestmentAmount = 0L;
    long firstMonthSpendingLimit = 0L;
    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
    List<CashflowForecastMonthCalculation> months = new ArrayList<>();
    List<Long> savingContributions = new ArrayList<>();
    List<Long> investmentContributions = new ArrayList<>();
    List<LocalDate> savingContributionDates = new ArrayList<>();
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    boolean hasAppliedStrategy = input.appliedStrategy() != null;
    if (hasAppliedStrategy
        && (input.appliedStrategy().expectedReturnRate() == null
            || input.appliedStrategy().expectedReturnRate().signum() < 0)) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 금액이 유효하지 않습니다.");
    }

    BigDecimal spendingRatio = BigDecimal.ZERO;
    BigDecimal investmentRatio = BigDecimal.ZERO;
    if (hasAppliedStrategy) {
      long firstMonthSalary =
          militaryPayPolicy
              .resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), startMonth)
              .monthlySalary();
      if (firstMonthSalary > 0L) {
        spendingRatio =
            BigDecimal.valueOf(input.appliedStrategy().monthlySpendingAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, java.math.RoundingMode.HALF_UP);
        investmentRatio =
            BigDecimal.valueOf(input.appliedStrategy().monthlyInvestmentAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, java.math.RoundingMode.HALF_UP);
      }
    }

    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      int remainingMonths = totalMonths - index;
      DefaultMilitaryPayPolicy.MilitaryPay pay =
          militaryPayPolicy.resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), month);
      long requiredSaving = requiredSaving(input.targetAmount(), asset, remainingMonths);
      long spending =
          hasAppliedStrategy
              ? scale(spendingRatio, pay.monthlySalary())
              : Math.max(0L, input.monthlySpendingAverage());
      long monthlySaving =
          hasAppliedStrategy
              ? input.appliedStrategy().monthlySavingAmount()
              : Math.min(DEFAULT_MONTHLY_SAVING_AMOUNT, pay.monthlySalary());
      long spendingLimit = Math.max(0L, pay.monthlySalary() - requiredSaving);
      long monthlyInvestment =
          hasAppliedStrategy
              ? scale(investmentRatio, pay.monthlySalary())
              : Math.max(
                  0L,
                  Math.min(
                      pay.monthlySalary() - spending - monthlySaving,
                      requiredSaving - monthlySaving));
      if (hasAppliedStrategy) {
        validateAppliedStrategy(spending, monthlySaving, monthlyInvestment, pay.monthlySalary());
      }

      long assetBeforeMonth = asset;
      ConservativeMonthlyCashflowEngine.MonthProjection projection =
          cashflowEngine.project(
              assetBeforeMonth,
              pay.monthlySalary(),
              spending,
              input.targetAmount(),
              calculationDate,
              input.dischargeDate(),
              month);
      asset = projection.endingAsset();
      expectedSalary += pay.monthlySalary();
      expectedSpending += spending;
      expectedSavingAmount += monthlySaving;
      expectedInvestmentAmount += monthlyInvestment;
      savingContributions.add(monthlySaving);
      investmentContributions.add(monthlyInvestment);
      savingContributionDates.add(month.atDay(1));
      if (index == 0) firstMonthSpendingLimit = spendingLimit;
      if (financialDischargeDate == null && projection.targetReachedDate() != null) {
        financialDischargeDate = projection.targetReachedDate();
      }
      months.add(
          new CashflowForecastMonthCalculation(
              month.atDay(1),
              pay.rankName(),
              pay.monthlySalary(),
              monthlySaving,
              monthlyInvestment,
              spending,
              asset));
    }

    BigDecimal investmentAnnualReturnRate =
        hasAppliedStrategy ? input.appliedStrategy().expectedReturnRate() : BigDecimal.ZERO;
    BigDecimal finalInvestmentRatio = investmentRatio;
    long existingInvestmentPrincipal =
        investmentPrincipalProvider
            .resolveLinkedPrincipal(input.userId())
            .orElseGet(
                () ->
                    investmentPrincipalBackfill.estimate(
                        input.soldierType(),
                        input.enlistmentDate(),
                        calculationDate,
                        finalInvestmentRatio));
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        cashflowEngine.calculateProjectedBenefit(
            input.soldierSavings(),
            calculationDate,
            savingContributions,
            savingContributionDates,
            input.dischargeDate(),
            existingInvestmentPrincipal,
            investmentContributions,
            investmentAnnualReturnRate);
    long expectedAsset = Math.addExact(asset, benefit.projectedBenefitAmount());
    if (financialDischargeDate == null && expectedAsset >= input.targetAmount()) {
      financialDischargeDate = input.dischargeDate();
    }

    return new CashflowForecastCalculation(
        expectedSalary,
        expectedSpending,
        expectedSavingAmount,
        expectedInvestmentAmount,
        expectedAsset,
        benefit.soldierSavingPrincipal(),
        benefit.soldierSavingInterest(),
        benefit.governmentMatchingSupport(),
        benefit.investmentPrincipal(),
        benefit.expectedInvestmentReturn(),
        benefit.projectedBenefitAmount(),
        ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION,
        firstMonthSpendingLimit,
        achievementRate(expectedAsset, input.targetAmount()),
        financialDischargeDate,
        List.copyOf(months));
  }

  private long requiredSaving(long targetAmount, long asset, int remainingMonths) {
    long remainingAmount = Math.max(0L, targetAmount - asset);
    return (remainingAmount + remainingMonths - 1) / remainingMonths;
  }

  private long scale(BigDecimal ratio, long referenceSalary) {
    return ratio
        .multiply(BigDecimal.valueOf(referenceSalary))
        .setScale(0, java.math.RoundingMode.HALF_UP)
        .longValueExact();
  }

  private void validateAppliedStrategy(
      long spending, long saving, long investment, long referenceMonthlyIncome) {
    if (spending < 0
        || saving < 0
        || saving > DEFAULT_MONTHLY_SAVING_AMOUNT
        || investment < 0) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 금액이 유효하지 않습니다.");
    }
    try {
      long allocated =
          Math.addExact(
              Math.addExact(spending, saving), investment);
      if (allocated > referenceMonthlyIncome) {
        throw new CashflowException(
            CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 합계가 해당 월 군 월급을 초과합니다.");
      }
    } catch (ArithmeticException exception) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 합계를 계산할 수 없습니다.");
    }
  }

  private double achievementRate(long expectedAsset, long targetAmount) {
    if (targetAmount == 0L) return 100D;
    return Math.min(999.99D, expectedAsset * 100D / targetAmount);
  }
}
