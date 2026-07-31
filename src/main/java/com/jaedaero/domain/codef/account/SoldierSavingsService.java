package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Finds military tomorrow savings accounts from the account list already synchronized from CODEF. */
@Service
public class SoldierSavingsService {

  private static final String SOLDIER_SAVINGS_PRODUCT_NAME = "장병내일준비적금";

  private final CodefPersistenceRepository repository;

  public SoldierSavingsService(CodefPersistenceRepository repository) {
    this.repository = repository;
  }

  public SoldierSavingsResponse getSoldierSavings(long userId) {
    List<ConnectedAccountResponse> accounts =
        repository.findAccountsByUserId(userId).stream()
            .filter(account -> isSoldierSavingsProduct(account.productName()))
            .map(ConnectedAccountResponse::new)
            .collect(Collectors.toList());
    return new SoldierSavingsResponse(!accounts.isEmpty(), accounts);
  }

  private boolean isSoldierSavingsProduct(String productName) {
    return productName != null
        && productName.replaceAll("\\s", "").contains(SOLDIER_SAVINGS_PRODUCT_NAME);
  }
}
