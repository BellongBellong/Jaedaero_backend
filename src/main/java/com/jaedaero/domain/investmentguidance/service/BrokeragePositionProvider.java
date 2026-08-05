package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;

public interface BrokeragePositionProvider {

  BrokeragePositionSnapshot load(long userId, RecurringInvestmentPlanVo plan);
}
