package com.jaedaero.domain.report.service;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.service.DashboardService;
import com.jaedaero.domain.report.dto.DischargeReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DischargeReportServiceImpl implements DischargeReportService {

  private final DashboardService dashboardService;
  private final CashflowService cashflowService;

  @Override
  @Transactional
  public DischargeReportResponse get(long userId) {
    // DashboardService generates an initial forecast when none exists.
    DashboardResponse dashboard = dashboardService.get(userId);
    CashflowForecastResponse cashflow = cashflowService.getLatest(userId);
    return DischargeReportResponse.from(cashflow, dashboard);
  }
}
