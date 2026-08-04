package com.jaedaero.domain.dashboard.service;

import com.jaedaero.domain.dashboard.dto.DashboardResponse;

public interface DashboardService {

  DashboardResponse get(long userId);
}
