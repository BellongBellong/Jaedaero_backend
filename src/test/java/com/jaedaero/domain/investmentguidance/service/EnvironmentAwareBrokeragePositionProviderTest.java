package com.jaedaero.domain.investmentguidance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EnvironmentAwareBrokeragePositionProviderTest {

  @Test
  void usesMockDbProviderInLocalEnvironment() {
    BrokeragePositionSnapshot mockResult = snapshot(1_100_000L);
    BrokeragePositionProvider mockProvider = (userId, plan) -> mockResult;
    BrokeragePositionProvider codefProvider =
        (userId, plan) -> {
          throw new AssertionError("local 환경에서 CODEF를 호출하면 안 됩니다.");
        };
    EnvironmentAwareBrokeragePositionProvider provider =
        new EnvironmentAwareBrokeragePositionProvider(mockProvider, codefProvider, "local");

    assertEquals(mockResult, provider.load(1L, plan()));
  }

  @Test
  void usesCodefProviderOutsideLocalEnvironment() {
    BrokeragePositionSnapshot codefResult = snapshot(2_200_000L);
    BrokeragePositionProvider mockProvider =
        (userId, plan) -> {
          throw new AssertionError("운영 환경에서 mock DB를 조회하면 안 됩니다.");
        };
    BrokeragePositionProvider codefProvider = (userId, plan) -> codefResult;
    EnvironmentAwareBrokeragePositionProvider provider =
        new EnvironmentAwareBrokeragePositionProvider(mockProvider, codefProvider, "production");

    assertEquals(codefResult, provider.load(1L, plan()));
  }

  private BrokeragePositionSnapshot snapshot(long accountValue) {
    return new BrokeragePositionSnapshot(
        accountValue,
        0L,
        accountValue,
        accountValue,
        accountValue,
        0L,
        BigDecimal.ZERO.setScale(4),
        null);
  }

  private RecurringInvestmentPlanVo plan() {
    return RecurringInvestmentPlanVo.builder().brokerageAccountId(7L).build();
  }
}
