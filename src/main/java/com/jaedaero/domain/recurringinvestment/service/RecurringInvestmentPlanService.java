package com.jaedaero.domain.recurringinvestment.service;

import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanRequest;
import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanResponse;

public interface RecurringInvestmentPlanService {

  RecurringInvestmentPlanResponse get(long userId);

  RecurringInvestmentPlanResponse save(long userId, RecurringInvestmentPlanRequest request);
}
