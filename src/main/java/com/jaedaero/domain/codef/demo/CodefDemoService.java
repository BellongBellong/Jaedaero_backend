package com.jaedaero.domain.codef.demo;

import com.jaedaero.domain.codef.account.CodefAccountSyncService;
import com.jaedaero.domain.codef.account.CodefSavingsTransactionSyncService;
import com.jaedaero.domain.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.domain.codef.account.CodefTransactionSyncService;
import com.jaedaero.domain.codef.account.SecuritiesAssetResponse;
import com.jaedaero.domain.codef.account.SecuritiesHoldingResponse;
import com.jaedaero.domain.codef.account.TransactionCategory;
import com.jaedaero.domain.codef.connection.CodefBankConnectionCreateRequest;
import com.jaedaero.domain.codef.connection.CodefConnectionService;
import com.jaedaero.domain.codef.institution.CodefBankInstitution;
import com.jaedaero.domain.codef.institution.CodefBusinessType;
import com.jaedaero.domain.codef.institution.CodefSecuritiesInstitution;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import com.jaedaero.domain.codef.persistence.StoredInstitutionConnection;
import com.jaedaero.domain.codef.persistence.StoredTransaction;
import com.jaedaero.global.security.SensitiveValueCipher;
import com.jaedaero.global.security.Sha256Hasher;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Orchestrates the server-rendered, local-only CODEF account/transaction demonstration. */
@Service
public class CodefDemoService {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final String MOCK_SAVING_ACCOUNT_NUMBER = "999-00-000001";
  private static final String MOCK_SAVING_ACCOUNT_MASKED = "999-00-000001";
  private static final String MOCK_SAVING_PRODUCT_NAME = "장병내일준비적금 (데모)";

  private final CodefPersistenceRepository repository;
  private final CodefConnectionService connectionService;
  private final CodefTransactionSyncService transactionSyncService;
  private final CodefSavingsTransactionSyncService savingsTransactionSyncService;
  private final CodefSecuritiesInquiryService securitiesInquiryService;
  private final CodefAccountSyncService accountSyncService;
  private final SensitiveValueCipher cipher;
  private final Sha256Hasher hasher;

  public CodefDemoService(
      CodefPersistenceRepository repository,
      CodefConnectionService connectionService,
      CodefTransactionSyncService transactionSyncService,
      CodefSavingsTransactionSyncService savingsTransactionSyncService,
      CodefSecuritiesInquiryService securitiesInquiryService,
      CodefAccountSyncService accountSyncService,
      SensitiveValueCipher cipher,
      Sha256Hasher hasher) {
    this.repository = repository;
    this.connectionService = connectionService;
    this.transactionSyncService = transactionSyncService;
    this.savingsTransactionSyncService = savingsTransactionSyncService;
    this.securitiesInquiryService = securitiesInquiryService;
    this.accountSyncService = accountSyncService;
    this.cipher = cipher;
    this.hasher = hasher;
  }

  /** Uses cached accounts when this demo user already connected the selected institution. */
  public void connect(long userId, CodefDemoLoginForm form) {
    CodefBusinessType businessType = CodefBusinessType.fromCode(form.getBusinessType());
    if (businessType == CodefBusinessType.BANK) {
      CodefBankInstitution.fromOrganizationCode(form.getOrganizationCode());
    } else {
      CodefSecuritiesInstitution.fromOrganizationCode(form.getOrganizationCode());
    }
    repository.createDemoUserIfAbsent(userId);
    CodefBankConnectionCreateRequest request = new CodefBankConnectionCreateRequest();
    request.setUserId(userId);
    request.setOrganizationCode(form.getOrganizationCode());
    request.setBusinessType(businessType.getCode());
    request.setLoginId(form.getLoginId());
    request.setPassword(form.getPassword());
    request.setBirthDate(form.getBirthDate());
    connectionService.connect(userId, request);
  }

  /** Refreshes a prior institution with its CODEF Connected ID, without a password form value. */
  public void loadSavedInstitution(long userId, String organizationCode, String businessType) {
    repository.createDemoUserIfAbsent(userId);
    connectionService.syncRegisteredInstitution(userId, organizationCode, businessType);
  }

  public List<CodefDemoSavedInstitution> getSavedInstitutions(long userId) {
    repository.createDemoUserIfAbsent(userId);
    seedMockMilitarySavingsIfConnected(userId);
    List<CodefDemoSavedInstitution> savedInstitutions = new ArrayList<>();
    for (StoredInstitutionConnection connection :
        repository.findActiveInstitutionConnectionsByUserId(userId)) {
      savedInstitutions.add(
          new CodefDemoSavedInstitution(
              connection.institutionCode(),
              connection.businessType(),
              institutionName(connection.institutionCode(), connection.businessType()),
              maskedLoginId(connection.loginIdEncrypted())));
    }
    return savedInstitutions;
  }

  /** Refreshes all connected institutions and calculates each account's current asset amount. */
  public CodefDemoUnifiedAssets getUnifiedAssets(long userId) {
    int refreshedAccountCount = accountSyncService.refreshAllAccounts(userId);
    long totalAmount = 0L;
    Map<String, List<CodefDemoUnifiedAccountAsset>> itemsByInstitution = new LinkedHashMap<>();
    for (StoredConnectedAccount account : repository.findAccountsByUserId(userId)) {
      if ("대출".equals(account.accountType())) {
        continue;
      }
      long amount = account.currentBalance();
      String statusMessage = "은행 잔액 기준";
      if (CodefBusinessType.SECURITIES.getCode().equals(account.businessType())) {
        try {
          SecuritiesAssetResponse response =
              securitiesInquiryService.getFinancialAssets(userId, account.accountId());
          amount = response.depositAmount();
          for (SecuritiesHoldingResponse holding : response.holdings()) {
            amount += holding.valuationAmount();
          }
          statusMessage = "증권 평가자산 기준";
        } catch (RuntimeException exception) {
          statusMessage = "증권 자산 조회 실패";
        }
      }
      totalAmount += amount;
      boolean securities = CodefBusinessType.SECURITIES.getCode().equals(account.businessType());
      boolean bankDetailAvailable =
          "DEMAND_DEPOSIT".equals(account.accountType())
              || "INSTALLMENT_SAVINGS".equals(account.accountType());
      String transactionKind =
          "INSTALLMENT_SAVINGS".equals(account.accountType())
              ? "INSTALLMENT_SAVINGS"
              : "DEMAND_DEPOSIT";
      String institutionName = institutionName(account.institutionCode(), account.businessType());
      itemsByInstitution
          .computeIfAbsent(institutionName, ignored -> new ArrayList<>())
          .add(
              new CodefDemoUnifiedAccountAsset(
                  account.accountId(),
                  institutionName,
                  account.productName(),
                  account.accountMasked(),
                  formatAmount(amount),
                  statusMessage,
                  securities,
                  bankDetailAvailable,
                  transactionKind));
    }
    List<CodefDemoInstitutionAssetGroup> institutionGroups = new ArrayList<>();
    for (Map.Entry<String, List<CodefDemoUnifiedAccountAsset>> entry :
        itemsByInstitution.entrySet()) {
      institutionGroups.add(new CodefDemoInstitutionAssetGroup(entry.getKey(), entry.getValue()));
    }
    return new CodefDemoUnifiedAssets(
        formatAmount(totalAmount), refreshedAccountCount, institutionGroups);
  }

  public CodefDemoAccountOverview getAccountOverview(
      long userId, String organizationCode, String businessType) {
    List<CodefDemoAccountCard> cards = new ArrayList<>();
    List<StoredConnectedAccount> accounts =
        repository.findAccountsByUserIdAndInstitution(userId, organizationCode, businessType);
    for (StoredConnectedAccount account : accounts) {
      addCard(cards, account);
    }
    return new CodefDemoAccountOverview(cards, findMilitarySavings(accounts));
  }

  private CodefDemoSavingsStatus findMilitarySavings(List<StoredConnectedAccount> accounts) {
    List<CodefDemoSavingsCard> militarySavings = new ArrayList<>();
    for (StoredConnectedAccount account : accounts) {
      if (!isMilitaryTomorrowSavings(account.productName())) {
        continue;
      }
      LocalDate maturityDate = parseDate(account.maturityDate());
      boolean matured = maturityDate != null && !maturityDate.isAfter(LocalDate.now(KOREA_ZONE));
      militarySavings.add(
          new CodefDemoSavingsCard(
              account.productName(),
              account.accountMasked(),
              formatAmount(account.currentBalance()),
              maturityDate == null ? "만기일 정보 없음" : maturityDate.toString(),
              matured));
    }

    if (militarySavings.isEmpty()) {
      return new CodefDemoSavingsStatus(
          CodefDemoSavingsStatus.Result.NOT_FOUND,
          "연동된 은행 계좌에서는 장병내일준비적금을 찾지 못했습니다.",
          militarySavings);
    }

    boolean hasActiveSavings = militarySavings.stream().anyMatch(card -> !card.isMatured());
    if (hasActiveSavings) {
      return new CodefDemoSavingsStatus(
          CodefDemoSavingsStatus.Result.ACTIVE, "장병내일준비적금 가입 상태가 확인되었습니다.", militarySavings);
    }

    return new CodefDemoSavingsStatus(
        CodefDemoSavingsStatus.Result.MATURED, "장병내일준비적금이 모두 만기된 상태입니다.", militarySavings);
  }

  private boolean isMilitaryTomorrowSavings(String productName) {
    return productName.replaceAll("\\s", "").contains("장병내일준비적금");
  }

  private LocalDate parseDate(String value) {
    if (value == null || !value.matches("\\d{8}")) {
      return null;
    }
    try {
      return LocalDate.parse(value, BASIC_DATE);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  public List<CodefDemoTransaction> getRecentTransactions(
      long userId, long accountId, String transactionKind, LocalDate startDate, LocalDate endDate) {
    seedMockMilitarySavingsIfConnected(userId);
    String inquiryType =
        "INSTALLMENT_SAVINGS".equals(transactionKind) ? "INSTALLMENT_SAVINGS" : "DEMAND_DEPOSIT";
    StoredConnectedAccount account =
        repository
            .findAccountByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new IllegalArgumentException("연결된 계좌를 찾을 수 없습니다."));
    if (!inquiryType.equals(account.accountType())) {
      throw new IllegalArgumentException("선택한 계좌의 거래 유형이 올바르지 않습니다.");
    }
    if (!isMockMilitarySaving(account)
        && !repository.isTransactionPeriodCovered(accountId, inquiryType, startDate, endDate)) {
      String startDateText = startDate.format(DateTimeFormatter.BASIC_ISO_DATE);
      String endDateText = endDate.format(DateTimeFormatter.BASIC_ISO_DATE);
      if ("INSTALLMENT_SAVINGS".equals(inquiryType)) {
        savingsTransactionSyncService.sync(userId, accountId, startDateText, endDateText);
      } else {
        transactionSyncService.sync(userId, accountId, startDateText, endDateText);
      }
      repository.recordTransactionSyncPeriod(accountId, inquiryType, startDate, endDate);
    }
    List<CodefDemoTransaction> transactions = new ArrayList<>();
    for (StoredTransaction transaction :
        repository.findTransactions(accountId, startDate, endDate)) {
      boolean deposit = "DEPOSIT".equals(transaction.transactionType());
      transactions.add(
          new CodefDemoTransaction(
              transaction.transactionId(),
              DATE_FORMAT.format(transaction.transactionAt().toLocalDate()),
              TIME_FORMAT.format(transaction.transactionAt().toLocalTime()),
              transaction.description(),
              formatAmount(transaction.amount()),
              transaction.balanceAfter() == null ? "-" : formatAmount(transaction.balanceAfter()),
              deposit,
              normalizedCategory(transaction.category())));
    }
    return transactions;
  }

  public String getAccountDisplay(long userId, long accountId) {
    return repository
        .findAccountByIdAndUserId(accountId, userId)
        .map(StoredConnectedAccount::accountMasked)
        .orElseThrow(() -> new IllegalArgumentException("연결된 계좌를 찾을 수 없습니다."));
  }

  public CodefDemoSecuritiesPortfolio getSecuritiesAssets(long userId, long accountId) {
    return toPortfolio(securitiesInquiryService.getFinancialAssets(userId, accountId));
  }

  public CodefDemoSecuritiesPortfolio getStockHoldings(long userId, long accountId) {
    return toPortfolio(securitiesInquiryService.getStockHoldings(userId, accountId));
  }

  private CodefDemoSecuritiesPortfolio toPortfolio(SecuritiesAssetResponse response) {
    List<CodefDemoSecuritiesHolding> holdings = new ArrayList<>();
    for (SecuritiesHoldingResponse holding : response.holdings()) {
      holdings.add(
          new CodefDemoSecuritiesHolding(
              holding.productType(),
              holding.itemName(),
              holding.itemCode(),
              emptyAsDash(holding.quantity()),
              formatAmount(holding.purchaseAmount()),
              formatAmount(holding.valuationAmount()),
              formatSignedAmount(holding.valuationProfit()),
              emptyAsDash(holding.earningsRate()),
              emptyAsDash(holding.currency())));
    }
    return new CodefDemoSecuritiesPortfolio(
        response.accountMasked(), formatAmount(response.depositAmount()), holdings);
  }

  private void addCard(List<CodefDemoAccountCard> cards, StoredConnectedAccount account) {
    boolean demandDeposit = "DEMAND_DEPOSIT".equals(account.accountType());
    boolean installmentSavings = "INSTALLMENT_SAVINGS".equals(account.accountType());
    cards.add(
        new CodefDemoAccountCard(
            account.accountId(),
            account.accountMasked(),
            account.productName(),
            category(account.accountType()),
            formatAmount(account.currentBalance()),
            demandDeposit || installmentSavings,
            installmentSavings ? "INSTALLMENT_SAVINGS" : "DEMAND_DEPOSIT"));
  }

  private String category(String accountType) {
    return switch (accountType) {
      case "DEMAND_DEPOSIT", "INSTALLMENT_SAVINGS" -> "예금·적금";
      case "대출" -> "대출";
      case "펀드" -> "펀드";
      case "외화" -> "외화";
      case "보험" -> "보험";
      default -> "기타";
    };
  }

  private String normalizedCategory(String category) {
    try {
      TransactionCategory resolved = TransactionCategory.from(category);
      return resolved == null ? TransactionCategory.ETC.name() : resolved.name();
    } catch (IllegalArgumentException exception) {
      return TransactionCategory.ETC.name();
    }
  }

  /**
   * Adds deterministic local-only data after the demo user has at least one real CODEF connection.
   * It is tied to the existing connection so foreign keys and ownership checks follow production flow.
   */
  public void seedMockMilitarySavingsIfConnected(long userId) {
    var connection = repository.findConnectionByUserId(userId);
    if (connection.isEmpty()) {
      return;
    }

    LocalDate today = LocalDate.now(KOREA_ZONE);
    String accountHash = hasher.hash(MOCK_SAVING_ACCOUNT_NUMBER);
    repository.upsertAccount(
        connection.get().connectionId(),
        "0301",
        CodefBusinessType.BANK.getCode(),
        "KB국민은행",
        cipher.encrypt(MOCK_SAVING_ACCOUNT_NUMBER),
        accountHash,
        MOCK_SAVING_ACCOUNT_MASKED,
        "INSTALLMENT_SAVINGS",
        MOCK_SAVING_PRODUCT_NAME,
        1_650_000L,
        1_650_000L,
        today.minusMonths(3),
        today.plusMonths(15));

    long accountId =
        repository
            .findAccountIdByConnectionAndAccountHash(connection.get().connectionId(), accountHash)
            .orElseThrow(() -> new IllegalStateException("데모 적금 계좌를 저장하지 못했습니다."));
    repository.upsertSoldierSaving(
        userId,
        accountId,
        "KB국민은행",
        550_000L,
        new BigDecimal("5.00"),
        today.minusMonths(3),
        today.plusMonths(15));

    insertMockSavingTransaction(accountId, today.minusMonths(2).withDayOfMonth(25), 550_000L);
    insertMockSavingTransaction(accountId, today.minusMonths(1).withDayOfMonth(25), 1_100_000L);
    insertMockSavingTransaction(accountId, today.withDayOfMonth(25), 1_650_000L);
  }

  private void insertMockSavingTransaction(long accountId, LocalDate date, long balanceAfter) {
    String externalKey = "demo-military-saving-" + date;
    String externalKeyHash = hasher.hash(externalKey);
    repository.insertTransactionIfAbsent(
        accountId,
        LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 9, 0),
        550_000L,
        balanceAfter,
        "DEPOSIT",
        "장병내일준비적금 월 납입",
        externalKeyHash);
    repository.fillTransactionCategoryIfEmpty(
        accountId, externalKeyHash, TransactionCategory.ASSET.name());
  }

  private boolean isMockMilitarySaving(StoredConnectedAccount account) {
    return MOCK_SAVING_PRODUCT_NAME.equals(account.productName())
        && MOCK_SAVING_ACCOUNT_MASKED.equals(account.accountMasked());
  }

  private String institutionName(String organizationCode, String businessType) {
    if (CodefBusinessType.BANK.getCode().equals(businessType)) {
      return CodefBankInstitution.fromOrganizationCode(organizationCode).getDisplayName();
    }
    return CodefSecuritiesInstitution.fromOrganizationCode(organizationCode).getDisplayName();
  }

  private String maskedLoginId(String encryptedLoginId) {
    if (encryptedLoginId == null || encryptedLoginId.isBlank()) {
      return "저장된 로그인 ID 없음";
    }
    String loginId = cipher.decrypt(encryptedLoginId);
    return loginId.length() <= 2 ? "••" : loginId.substring(0, 2) + "•••";
  }

  private String formatAmount(long amount) {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
  }

  private String formatSignedAmount(long amount) {
    return (amount > 0 ? "+" : "") + formatAmount(amount);
  }

  private String emptyAsDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
