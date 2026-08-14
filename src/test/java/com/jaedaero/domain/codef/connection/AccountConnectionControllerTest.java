package com.jaedaero.domain.codef.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.codef.account.CodefAccountSyncService;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class AccountConnectionControllerTest {

  @Test
  void deactivatesOnlyTheUsersActiveAccount() {
    CapturingRepository repository = new CapturingRepository();
    AccountConnectionController controller = new AccountConnectionController(null, repository, null);

    ResponseEntity<Void> response = controller.deactivateAccount(5L, 1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertEquals(5L, repository.accountId);
    assertEquals(1L, repository.userId);
  }

  @Test
  void rejectsAnAccountThatIsNotActiveOrDoesNotBelongToTheUser() {
    CapturingRepository repository = new CapturingRepository();
    repository.updateCount = 0;
    AccountConnectionController controller = new AccountConnectionController(null, repository, null);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.deactivateAccount(5L, 1L));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

  @Test
  void activatesOnlyTheUsersDisconnectedAccountAndRefreshesAccounts() {
    CapturingRepository repository = new CapturingRepository();
    CapturingAccountSyncService syncService = new CapturingAccountSyncService();
    AccountConnectionController controller =
        new AccountConnectionController(null, repository, syncService);

    ResponseEntity<Void> response = controller.activateAccount(5L, 1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertEquals(5L, repository.accountId);
    assertEquals(1L, repository.userId);
    assertEquals(1L, syncService.userId);
  }

  @Test
  void rejectsAnAccountThatIsNotDisconnectedOrDoesNotBelongToTheUserWhenActivating() {
    CapturingRepository repository = new CapturingRepository();
    repository.updateCount = 0;
    AccountConnectionController controller =
        new AccountConnectionController(null, repository, new CapturingAccountSyncService());

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.activateAccount(5L, 1L));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

  private static class CapturingRepository extends CodefPersistenceRepository {
    private long accountId;
    private long userId;
    private int updateCount = 1;

    private CapturingRepository() {
      super(null);
    }

    @Override
    public int deactivateAccountByIdAndUserId(long accountId, long userId) {
      this.accountId = accountId;
      this.userId = userId;
      return updateCount;
    }

    @Override
    public int activateAccountByIdAndUserId(long accountId, long userId) {
      this.accountId = accountId;
      this.userId = userId;
      return updateCount;
    }
  }

  private static class CapturingAccountSyncService extends CodefAccountSyncService {
    private long userId;

    private CapturingAccountSyncService() {
      super(null, null, null, null);
    }

    @Override
    public int refreshAllAccounts(long userId) {
      this.userId = userId;
      return 0;
    }
  }
}
