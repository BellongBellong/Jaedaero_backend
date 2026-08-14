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
  void refreshesAccountsBeforeActivatingTheUsersDisconnectedAccount() {
    CapturingRepository repository = new CapturingRepository();
    CapturingAccountSyncService syncService = new CapturingAccountSyncService(repository);
    AccountConnectionController controller =
        new AccountConnectionController(null, repository, syncService);

    ResponseEntity<Void> response = controller.activateAccount(5L, 1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertEquals(5L, repository.accountId);
    assertEquals(1L, repository.userId);
    assertEquals(1L, syncService.userId);
    assertEquals(true, repository.activatedAfterSync);
  }

  @Test
  void doesNotActivateAccountWhenRefreshFails() {
    CapturingRepository repository = new CapturingRepository();
    AccountConnectionController controller =
        new AccountConnectionController(null, repository, new FailingAccountSyncService());

    assertThrows(IllegalStateException.class, () -> controller.activateAccount(5L, 1L));

    assertEquals(0L, repository.accountId);
  }

  @Test
  void rejectsAnAccountThatIsNotDisconnectedOrDoesNotBelongToTheUserWhenActivating() {
    CapturingRepository repository = new CapturingRepository();
    repository.updateCount = 0;
    AccountConnectionController controller =
        new AccountConnectionController(null, repository, new CapturingAccountSyncService(repository));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.activateAccount(5L, 1L));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

  private static class CapturingRepository extends CodefPersistenceRepository {
    private long accountId;
    private long userId;
    private int updateCount = 1;
    private boolean synced;
    private boolean activatedAfterSync;

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
      this.activatedAfterSync = synced;
      return updateCount;
    }
  }

  private static class CapturingAccountSyncService extends CodefAccountSyncService {
    private long userId;
    private final CapturingRepository repository;

    private CapturingAccountSyncService(CapturingRepository repository) {
      super(null, null, null, null);
      this.repository = repository;
    }

    @Override
    public int refreshAllAccounts(long userId) {
      this.userId = userId;
      repository.synced = true;
      return 0;
    }
  }

  private static class FailingAccountSyncService extends CodefAccountSyncService {
    private FailingAccountSyncService() {
      super(null, null, null, null);
    }

    @Override
    public int refreshAllAccounts(long userId) {
      throw new IllegalStateException("Sensitive value decryption failed.");
    }
  }
}
