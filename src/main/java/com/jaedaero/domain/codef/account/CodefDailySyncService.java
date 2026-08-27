package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 활성 CODEF 연결의 계좌와 최근 거래내역을 일괄 적재합니다. */
@Slf4j
@Service
public class CodefDailySyncService {

  private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private static final int TRANSACTION_LOOKBACK_DAYS = 7;

  private final CodefPersistenceRepository repository;
  private final CodefAccountSyncService accountSyncService;
  private final CodefTransactionSyncService transactionSyncService;
  private final CodefSavingsTransactionSyncService savingsTransactionSyncService;
  private final CodefSecuritiesCashTransactionSyncService securitiesCashTransactionSyncService;
  private final Clock clock;

  public CodefDailySyncService(
      CodefPersistenceRepository repository,
      CodefAccountSyncService accountSyncService,
      CodefTransactionSyncService transactionSyncService,
      CodefSavingsTransactionSyncService savingsTransactionSyncService,
      CodefSecuritiesCashTransactionSyncService securitiesCashTransactionSyncService,
      Clock clock) {
    this.repository = repository;
    this.accountSyncService = accountSyncService;
    this.transactionSyncService = transactionSyncService;
    this.savingsTransactionSyncService = savingsTransactionSyncService;
    this.securitiesCashTransactionSyncService = securitiesCashTransactionSyncService;
    this.clock = clock;
  }

  public void syncAllActiveConnections() {
    for (long userId : repository.findActiveConnectionUserIds()) {
      syncUser(userId);
    }
  }

  void syncUser(long userId) {
    try {
      accountSyncService.refreshAllAccounts(userId);
    } catch (RuntimeException exception) {
      log.warn("CODEF 계좌 목록 일괄 동기화 실패: userId={}", userId, exception);
    }

    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = endDate.minusDays(TRANSACTION_LOOKBACK_DAYS);
    String start = startDate.format(BASIC_DATE);
    String end = endDate.format(BASIC_DATE);
    for (StoredConnectedAccount account : repository.findAccountsByUserId(userId)) {
      try {
        syncTransactions(userId, account, start, end, startDate, endDate);
      } catch (RuntimeException exception) {
        log.warn("CODEF 거래내역 일괄 동기화 실패: userId={}, accountId={}", userId, account.accountId(), exception);
      }
    }
  }

  private void syncTransactions(
      long userId,
      StoredConnectedAccount account,
      String start,
      String end,
      LocalDate startDate,
      LocalDate endDate) {
    if (repository.isDemoSoldierSavingAccount(account.accountId())) {
      return;
    }

    if ("ST".equals(account.businessType())) {
      securitiesCashTransactionSyncService.sync(userId, account.accountId(), start, end);
      repository.recordTransactionSyncPeriod(
          account.accountId(), "SECURITIES_CASH", startDate, endDate);
      return;
    }
    if ("INSTALLMENT_SAVINGS".equals(account.accountType())) {
      savingsTransactionSyncService.sync(userId, account.accountId(), start, end);
      repository.recordTransactionSyncPeriod(
          account.accountId(), "INSTALLMENT_SAVINGS", startDate, endDate);
      return;
    }
    if ("DEMAND_DEPOSIT".equals(account.accountType())) {
      transactionSyncService.sync(userId, account.accountId(), start, end);
      repository.recordTransactionSyncPeriod(account.accountId(), "DEMAND_DEPOSIT", startDate, endDate);
    }
  }
}
