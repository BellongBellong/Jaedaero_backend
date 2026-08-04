package com.jaedaero.domain.dashboard.service;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

  private final CashflowService cashflowService;
  private final DashboardMapper dashboardMapper;
  private final Clock clock;

  @Autowired
  public DashboardServiceImpl(CashflowService cashflowService, DashboardMapper dashboardMapper) {
    this(cashflowService, dashboardMapper, Clock.systemDefaultZone());
  }

  public DashboardServiceImpl(
      CashflowService cashflowService, DashboardMapper dashboardMapper, Clock clock) {
    this.cashflowService = cashflowService;
    this.dashboardMapper = dashboardMapper;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public DashboardResponse get(long userId) {
    CashflowForecastResponse cashflow = cashflowService.getLatest(userId);
    return DashboardResponse.from(
        cashflow, dashboardMapper.findActualDischargeDateByUserId(userId), LocalDate.now(clock));
  }
}
