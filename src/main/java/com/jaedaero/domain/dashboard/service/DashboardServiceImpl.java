package com.jaedaero.domain.dashboard.service;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.dashboard.mapper.DashboardSpendingMapper;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

  private final CashflowService cashflowService;
  private final DashboardMapper dashboardMapper;
  private final DashboardSpendingMapper dashboardSpendingMapper;
  private final StrategyApplicationMapper strategyApplicationMapper;
  private final Clock clock;

  @Autowired
  public DashboardServiceImpl(
      CashflowService cashflowService,
      DashboardMapper dashboardMapper,
      StrategyApplicationMapper strategyApplicationMapper,
      DashboardSpendingMapper dashboardSpendingMapper) {
    this(
        cashflowService,
        dashboardMapper,
        strategyApplicationMapper,
        dashboardSpendingMapper,
        Clock.systemDefaultZone());
  }

  public DashboardServiceImpl(
      CashflowService cashflowService,
      DashboardMapper dashboardMapper,
      StrategyApplicationMapper strategyApplicationMapper,
      DashboardSpendingMapper dashboardSpendingMapper,
      Clock clock) {
    this.cashflowService = cashflowService;
    this.dashboardMapper = dashboardMapper;
    this.strategyApplicationMapper = strategyApplicationMapper;
    this.dashboardSpendingMapper = dashboardSpendingMapper;
    this.clock = clock;
  }

  @Override
  public DashboardResponse get(long userId) {
    CashflowForecastResponse cashflow = latestOrGenerate(userId);
    return DashboardResponse.from(
        cashflow,
        dashboardMapper.findActualDischargeDateByUserId(userId),
        strategyApplicationMapper.findLatestByUserId(userId),
        LocalDate.now(clock),
        dashboardSpendingMapper.sumThisMonthSpendingByUserId(userId));
  }

  private CashflowForecastResponse latestOrGenerate(long userId) {
    try {
      return cashflowService.getLatest(userId);
    } catch (CashflowException exception) {
      if (exception.getErrorCode() != CashflowErrorCode.NOT_FOUND) {
        throw exception;
      }
      return cashflowService.generate(userId);
    }
  }
}
