package com.jaedaero.domain.cashflow.service.impl;

import com.jaedaero.domain.cashflow.dto.CashflowForecastMonthResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.mapper.CashflowMapper;
import com.jaedaero.domain.cashflow.service.CashflowCalculator;
import com.jaedaero.domain.cashflow.service.CashflowForecastCalculation;
import com.jaedaero.domain.cashflow.service.CashflowForecastMonthCalculation;
import com.jaedaero.domain.cashflow.service.CashflowInput;
import com.jaedaero.domain.cashflow.service.CashflowInputProvider;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.cashflow.vo.CashflowForecastMonthVo;
import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashflowServiceImpl implements CashflowService {

  private final CashflowMapper cashflowMapper;
  private final CashflowInputProvider cashflowInputProvider;
  private final CashflowCalculator cashflowCalculator;
  private final Clock clock;

  @Autowired
  public CashflowServiceImpl(
      CashflowMapper cashflowMapper,
      CashflowInputProvider cashflowInputProvider,
      CashflowCalculator cashflowCalculator) {
    this(cashflowMapper, cashflowInputProvider, cashflowCalculator, Clock.systemDefaultZone());
  }

  public CashflowServiceImpl(
      CashflowMapper cashflowMapper,
      CashflowInputProvider cashflowInputProvider,
      CashflowCalculator cashflowCalculator,
      Clock clock) {
    this.cashflowMapper = cashflowMapper;
    this.cashflowInputProvider = cashflowInputProvider;
    this.cashflowCalculator = cashflowCalculator;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public CashflowCalculationInputResponse getCalculationInput(long userId) {
    return CashflowCalculationInputResponse.from(cashflowInputProvider.load(userId));
  }

  @Override
  @Transactional
  public CashflowForecastResponse generate(long userId) {
    CashflowInput input = cashflowInputProvider.load(userId);
    CashflowForecastCalculation calculation =
        cashflowCalculator.calculate(input, LocalDate.now(clock));
    CashflowForecastVo forecast =
        CashflowForecastVo.builder()
            .userId(userId)
            .baseAsset(input.baseAsset())
            .expectedSalary(calculation.expectedSalary())
            .expectedSavingAmount(calculation.expectedSavingAmount())
            .expectedAsset(calculation.expectedAsset())
            .monthlySpendingLimit(calculation.monthlySpendingLimit())
            .achievementRate(BigDecimal.valueOf(calculation.achievementRate()).setScale(2, RoundingMode.HALF_UP))
            .financialDischargeDate(calculation.financialDischargeDate())
            .policyVersion(DefaultMilitaryPayPolicy.POLICY_VERSION)
            .build();
    cashflowMapper.insertForecast(forecast);

    List<CashflowForecastMonthVo> months =
        calculation.months().stream().map(this::toVo).toList();
    if (!months.isEmpty()) {
      cashflowMapper.insertForecastMonths(forecast.getForecastId(), months);
    }
    return CashflowForecastResponse.from(
        forecast, months.stream().map(CashflowForecastMonthResponse::from).toList());
  }

  @Override
  @Transactional(readOnly = true)
  public CashflowForecastResponse getLatest(long userId) {
    return getLatest(userId, Integer.MAX_VALUE);
  }

  @Override
  @Transactional(readOnly = true)
  public CashflowForecastResponse getLatest(long userId, int months) {
    CashflowForecastVo forecast = cashflowMapper.findLatestForecastByUserId(userId);
    if (forecast == null) {
      throw new CashflowException(CashflowErrorCode.NOT_FOUND, "생성된 캐시플로우 예측이 없습니다.");
    }
    List<CashflowForecastMonthResponse> forecastMonths =
        cashflowMapper.findMonthsByForecastId(forecast.getForecastId()).stream()
            .map(CashflowForecastMonthResponse::from)
            .limit(months)
            .toList();
    return CashflowForecastResponse.from(forecast, forecastMonths);
  }

  private CashflowForecastMonthVo toVo(CashflowForecastMonthCalculation month) {
    return CashflowForecastMonthVo.builder()
        .forecastMonth(month.forecastMonth())
        .expectedRank(month.expectedRank())
        .expectedSalary(month.expectedSalary())
        .expectedSavingAmount(month.expectedSavingAmount())
        .expectedInvestmentAmount(month.expectedInvestmentAmount())
        .expectedSpendingAmount(month.expectedSpendingAmount())
        .expectedEndingAsset(month.expectedEndingAsset())
        .build();
  }
}
