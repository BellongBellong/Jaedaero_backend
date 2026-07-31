package com.jaedaero.domain.codef.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.util.List;
import org.junit.jupiter.api.Test;

class SoldierSavingsServiceTest {

  @Test
  void returnsMatchingAccountsAndPossessionStatus() {
    SoldierSavingsService service =
        new SoldierSavingsService(
            new StubRepository(
                List.of(account(1L, "KB국민ONE적금"), account(2L, "장병 내일 준비 적금"))));

    SoldierSavingsResponse response = service.getSoldierSavings(1L);

    assertTrue(response.isHasSoldierSavings());
    assertEquals(1, response.getAccounts().size());
    assertEquals(2L, response.getAccounts().get(0).getAccountId());
  }

  @Test
  void returnsFalseAndAnEmptyListWhenNoMatchingAccountExists() {
    SoldierSavingsService service = new SoldierSavingsService(new StubRepository(List.of(account(1L, "일반 적금"))));

    SoldierSavingsResponse response = service.getSoldierSavings(1L);

    assertFalse(response.isHasSoldierSavings());
    assertTrue(response.getAccounts().isEmpty());
  }

  private static StoredConnectedAccount account(long accountId, String productName) {
    return new StoredConnectedAccount(
        accountId,
        1L,
        1L,
        "0004",
        "BK",
        "KB국민은행",
        "encrypted",
        "1234-****",
        "INSTALLMENT_SAVINGS",
        productName,
        100_000L,
        null,
        "20271231");
  }

  private static class StubRepository extends CodefPersistenceRepository {
    private final List<StoredConnectedAccount> accounts;

    StubRepository(List<StoredConnectedAccount> accounts) {
      super(null);
      this.accounts = accounts;
    }

    @Override
    public List<StoredConnectedAccount> findAccountsByUserId(long userId) {
      return accounts;
    }
  }
}
