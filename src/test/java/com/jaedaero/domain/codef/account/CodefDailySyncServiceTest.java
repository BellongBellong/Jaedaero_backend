package com.jaedaero.domain.codef.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodefDailySyncServiceTest {

  @Test
  void refreshesAccountsAndSyncsEachSupportedTransactionType() {
    RecordingRepository repository = new RecordingRepository();
    RecordingAccountSyncService accountSync = new RecordingAccountSyncService();
    RecordingTransactionSyncService transactionSync = new RecordingTransactionSyncService();
    RecordingSavingsSyncService savingsSync = new RecordingSavingsSyncService();
    RecordingSecuritiesSyncService securitiesSync = new RecordingSecuritiesSyncService();
    RecordingCategoryClassificationService categoryClassification =
        new RecordingCategoryClassificationService();
    CodefDailySyncService service =
        new CodefDailySyncService(
            repository,
            accountSync,
            transactionSync,
            savingsSync,
            securitiesSync,
            categoryClassification,
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    service.syncAllActiveConnections();

    assertEquals(List.of(1L), accountSync.userIds);
    assertEquals(List.of(10L), transactionSync.accountIds);
    assertEquals(List.of(11L), savingsSync.accountIds);
    assertEquals(List.of(12L), securitiesSync.accountIds);
    assertEquals(3, repository.recordedPeriods);
    assertEquals(1, categoryClassification.calls);
  }

  private static class RecordingRepository extends CodefPersistenceRepository {
    private int recordedPeriods;

    private RecordingRepository() {
      super(null);
    }

    @Override
    public List<Long> findActiveConnectionUserIds() {
      return List.of(1L);
    }

    @Override
    public List<StoredConnectedAccount> findAccountsByUserId(long userId) {
      return List.of(
          account(10L, "BK", "DEMAND_DEPOSIT"),
          account(11L, "BK", "INSTALLMENT_SAVINGS"),
          account(12L, "ST", "SECURITIES"));
    }

    @Override
    public void recordTransactionSyncPeriod(
        long accountId,
        String inquiryType,
        java.time.LocalDate startDate,
        java.time.LocalDate endDate) {
      recordedPeriods++;
    }

    private StoredConnectedAccount account(long accountId, String businessType, String accountType) {
      return new StoredConnectedAccount(
          accountId,
          1L,
          1L,
          "004",
          businessType,
          "테스트 기관",
          "encrypted",
          "123456-**-****78",
          accountType,
          "테스트 계좌",
          0L,
          null,
          null);
    }
  }

  private static class RecordingAccountSyncService extends CodefAccountSyncService {
    private final java.util.ArrayList<Long> userIds = new java.util.ArrayList<>();

    private RecordingAccountSyncService() {
      super(null, null, null, null);
    }

    @Override
    public int refreshAllAccounts(long userId) {
      userIds.add(userId);
      return 0;
    }
  }

  private static class RecordingTransactionSyncService extends CodefTransactionSyncService {
    private final java.util.ArrayList<Long> accountIds = new java.util.ArrayList<>();

    private RecordingTransactionSyncService() {
      super(null, null, null, null);
    }

    @Override
    public int sync(long userId, long accountId, String startDate, String endDate) {
      accountIds.add(accountId);
      return 0;
    }
  }

  private static class RecordingSavingsSyncService extends CodefSavingsTransactionSyncService {
    private final java.util.ArrayList<Long> accountIds = new java.util.ArrayList<>();

    private RecordingSavingsSyncService() {
      super(null, null, null, null);
    }

    @Override
    public int sync(long userId, long accountId, String startDate, String endDate) {
      accountIds.add(accountId);
      return 0;
    }
  }

  private static class RecordingSecuritiesSyncService
      extends CodefSecuritiesCashTransactionSyncService {
    private final java.util.ArrayList<Long> accountIds = new java.util.ArrayList<>();

    private RecordingSecuritiesSyncService() {
      super(null, null, null, null);
    }

    @Override
    public int sync(long userId, long accountId, String startDate, String endDate) {
      accountIds.add(accountId);
      return 0;
    }
  }

  private static class RecordingCategoryClassificationService
      extends TransactionCategoryAiClassificationService {
    private int calls;

    private RecordingCategoryClassificationService() {
      super(null, null, true, 1, 1);
    }

    @Override
    public int classifyPending() {
      calls++;
      return 0;
    }
  }
}
