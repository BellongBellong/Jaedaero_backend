package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Selects the mock DB provider locally and the CODEF provider in other environments. */
@Primary
@Component
public class EnvironmentAwareBrokeragePositionProvider implements BrokeragePositionProvider {

  private final BrokeragePositionProvider mockDbProvider;
  private final BrokeragePositionProvider codefProvider;
  private final String appEnvironment;

  public EnvironmentAwareBrokeragePositionProvider(
      @Qualifier("mockDbBrokeragePositionProvider") BrokeragePositionProvider mockDbProvider,
      @Qualifier("codefBrokeragePositionProvider") BrokeragePositionProvider codefProvider,
      @Value("${app.environment:production}") String appEnvironment) {
    this.mockDbProvider = mockDbProvider;
    this.codefProvider = codefProvider;
    this.appEnvironment = appEnvironment;
  }

  @Override
  public BrokeragePositionSnapshot load(long userId, RecurringInvestmentPlanVo plan) {
    return isLocal() ? mockDbProvider.load(userId, plan) : codefProvider.load(userId, plan);
  }

  private boolean isLocal() {
    return "local".equalsIgnoreCase(appEnvironment);
  }
}
