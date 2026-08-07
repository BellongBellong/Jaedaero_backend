package com.jaedaero.domain.investmentguidance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.investmentguidance.mapper.MockDbBrokeragePositionMapper;
import com.jaedaero.domain.investmentguidance.vo.MockDbBrokeragePositionSourceVo;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MockDbBrokeragePositionProviderTest {

  @Test
  void convertsAccountBalanceAndCashIntoLocalMockPosition() {
    LocalDateTime syncedAt = LocalDateTime.of(2026, 8, 5, 9, 0);
    MockDbBrokeragePositionSourceVo source = new MockDbBrokeragePositionSourceVo();
    source.setAccountValue(1_100_000L);
    source.setCashBalance(100_000L);
    source.setMarketDataAsOf(syncedAt);
    MockDbBrokeragePositionMapper mapper = (userId, accountId) -> source;
    MockDbBrokeragePositionProvider provider = new MockDbBrokeragePositionProvider(mapper);

    BrokeragePositionSnapshot result = provider.load(1L, plan(7L));

    assertEquals(1_100_000L, result.accountValue());
    assertEquals(1_000_000L, result.investmentPrincipal());
    assertEquals(1_000_000L, result.marketValue());
    assertEquals(0L, result.unrealizedProfitLoss());
    assertEquals(new BigDecimal("0.0000"), result.returnRate());
    assertEquals(syncedAt, result.marketDataAsOf());
  }

  @Test
  void rejectsMissingLocalMockSnapshot() {
    MockDbBrokeragePositionMapper mapper = (userId, accountId) -> null;
    MockDbBrokeragePositionProvider provider = new MockDbBrokeragePositionProvider(mapper);

    assertThrows(IllegalStateException.class, () -> provider.load(1L, plan(7L)));
  }

  private RecurringInvestmentPlanVo plan(long accountId) {
    return RecurringInvestmentPlanVo.builder().brokerageAccountId(accountId).build();
  }
}
