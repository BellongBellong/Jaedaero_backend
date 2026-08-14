package com.jaedaero.domain.cashflow.service;

import java.util.Optional;

/**
 * 특정 종목 매칭 없이 사용자의 연동 증권계좌 전체 매입원금을 합산합니다.
 *
 * <p>빈 Optional은 연동된 증권계좌가 없다는 뜻이며, 호출자는 계급별 월급 기반 백필로 대체한다.
 */
public interface AggregateInvestmentPrincipalProvider {

  Optional<Long> resolveLinkedPrincipal(long userId);
}
