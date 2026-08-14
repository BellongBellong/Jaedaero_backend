package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.domain.codef.account.SecuritiesHoldingResponse;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CodefAggregateInvestmentPrincipalProvider
    implements AggregateInvestmentPrincipalProvider {

  private static final String SECURITIES_BUSINESS_TYPE = "ST";

  private final CodefPersistenceRepository repository;
  private final CodefSecuritiesInquiryService securitiesInquiryService;

  public CodefAggregateInvestmentPrincipalProvider(
      CodefPersistenceRepository repository,
      CodefSecuritiesInquiryService securitiesInquiryService) {
    this.repository = repository;
    this.securitiesInquiryService = securitiesInquiryService;
  }

  @Override
  public Optional<Long> resolveLinkedPrincipal(long userId) {
    List<StoredConnectedAccount> securitiesAccounts =
        repository.findAccountsByUserId(userId).stream()
            .filter(account -> SECURITIES_BUSINESS_TYPE.equals(account.businessType()))
            .toList();
    if (securitiesAccounts.isEmpty()) {
      return Optional.empty();
    }
    long principal = 0L;
    for (StoredConnectedAccount account : securitiesAccounts) {
      List<SecuritiesHoldingResponse> holdings =
          securitiesInquiryService.getFinancialAssets(userId, account.accountId()).holdings();
      principal =
          Math.addExact(
              principal, holdings.stream().mapToLong(SecuritiesHoldingResponse::purchaseAmount).sum());
    }
    return Optional.of(principal);
  }
}
