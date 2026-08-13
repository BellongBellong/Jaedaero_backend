package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** local mock 환경에서는 매입원가가 없으므로 평가금액(잔액)을 원금으로 근사한다. */
@Component
public class LocalAggregateInvestmentPrincipalProvider
    implements AggregateInvestmentPrincipalProvider {

  private static final String SECURITIES_BUSINESS_TYPE = "ST";

  private final CodefPersistenceRepository repository;

  public LocalAggregateInvestmentPrincipalProvider(CodefPersistenceRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Long> resolveLinkedPrincipal(long userId) {
    var securitiesAccounts =
        repository.findAccountsByUserId(userId).stream()
            .filter(account -> SECURITIES_BUSINESS_TYPE.equals(account.businessType()))
            .toList();
    if (securitiesAccounts.isEmpty()) {
      return Optional.empty();
    }
    long principal =
        securitiesAccounts.stream().mapToLong(StoredConnectedAccount::currentBalance).sum();
    return Optional.of(principal);
  }
}
