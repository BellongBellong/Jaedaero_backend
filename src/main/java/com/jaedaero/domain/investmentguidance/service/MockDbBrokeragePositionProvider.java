package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.investmentguidance.mapper.MockDbBrokeragePositionMapper;
import com.jaedaero.domain.investmentguidance.vo.MockDbBrokeragePositionSourceVo;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reads the local mock brokerage account snapshot without calling CODEF. */
@Component
@RequiredArgsConstructor
public class MockDbBrokeragePositionProvider implements BrokeragePositionProvider {

  private final MockDbBrokeragePositionMapper mapper;

  @Override
  public BrokeragePositionSnapshot load(long userId, RecurringInvestmentPlanVo plan) {
    MockDbBrokeragePositionSourceVo source =
        mapper.findByUserIdAndAccountId(userId, plan.getBrokerageAccountId());
    if (source == null
        || source.getAccountValue() == null
        || source.getAccountValue() < 0
        || source.getMarketDataAsOf() == null) {
      throw new IllegalStateException("유효한 로컬 증권계좌 평가 스냅샷이 없습니다.");
    }

    long accountValue = source.getAccountValue();
    long cashBalance = source.getCashBalance() == null ? 0L : source.getCashBalance();
    cashBalance = Math.max(0L, Math.min(accountValue, cashBalance));
    long marketValue = accountValue - cashBalance;

    // connected_account에는 보유종목별 매입원가를 저장하지 않는다. 로컬 mock에서는
    // 평가금액을 투자원금으로 간주해 평가손익과 수익률을 0으로 고정한다.
    return new BrokeragePositionSnapshot(
        accountValue,
        marketValue,
        marketValue,
        0L,
        BigDecimal.ZERO.setScale(4),
        source.getMarketDataAsOf());
  }
}
