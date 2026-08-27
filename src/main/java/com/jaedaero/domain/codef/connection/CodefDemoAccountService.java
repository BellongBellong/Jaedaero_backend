package com.jaedaero.domain.codef.connection;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredCodefConnection;
import com.jaedaero.global.security.SensitiveValueCipher;
import com.jaedaero.global.security.Sha256Hasher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시연용 지정 로그인 ID에만 CODEF 호출 없이 계좌 데이터를 준비합니다. */
@Service
public class CodefDemoAccountService {

  private static final String BANK_BUSINESS_TYPE = "BK";
  private static final String SECURITIES_BUSINESS_TYPE = "ST";
  private static final String KOOKMIN_CODE = "0004";
  private static final String HANA_CODE = "0081";
  private static final String MIRAE_ASSET_CODE = "0238";
  private static final String DEMO_CONNECTION_PREFIX = "JAEDAERO_DEMO_CONNECTION_";

  private final CodefPersistenceRepository repository;
  private final SensitiveValueCipher cipher;
  private final Sha256Hasher hasher;
  private final boolean enabled;
  private final long demoUserId;
  private final String kookminLoginId;
  private final String hanaLoginId;
  private final String miraeAssetLoginId;

  public CodefDemoAccountService(
      CodefPersistenceRepository repository,
      SensitiveValueCipher cipher,
      Sha256Hasher hasher,
      @Value("${codef.demo.enabled:false}") boolean enabled,
      @Value("${codef.demo.user-id:-1}") long demoUserId,
      @Value("${codef.demo.kookmin-login-id:}") String kookminLoginId,
      @Value("${codef.demo.hana-login-id:}") String hanaLoginId,
      @Value("${codef.demo.mirae-asset-login-id:}") String miraeAssetLoginId) {
    this.repository = repository;
    this.cipher = cipher;
    this.hasher = hasher;
    this.enabled = enabled;
    this.demoUserId = demoUserId;
    this.kookminLoginId = kookminLoginId;
    this.hanaLoginId = hanaLoginId;
    this.miraeAssetLoginId = miraeAssetLoginId;
  }

  boolean isDemoUser(long userId) {
    return enabled && userId == demoUserId;
  }

  /** 시연 계좌는 지정 로그인 ID를 제출한 뒤에만 목록에 노출합니다. */
  boolean shouldHideAccountsUntilConnected(long userId) {
    return isDemoUser(userId) && !repository.isDemoConnectionActivated(userId);
  }

  boolean supports(long userId, CodefBankConnectionCreateRequest request) {
    if (!isDemoUser(userId)) {
      return false;
    }
    if (BANK_BUSINESS_TYPE.equals(request.getBusinessType())) {
      return (KOOKMIN_CODE.equals(request.getOrganizationCode())
              && matchesLoginId(request.getLoginId(), kookminLoginId))
          || (HANA_CODE.equals(request.getOrganizationCode())
              && matchesLoginId(request.getLoginId(), hanaLoginId));
    }
    return SECURITIES_BUSINESS_TYPE.equals(request.getBusinessType())
        && MIRAE_ASSET_CODE.equals(request.getOrganizationCode())
        && matchesLoginId(request.getLoginId(), miraeAssetLoginId);
  }

  @Transactional
  CodefConnectionResponse connect(long userId, CodefBankConnectionCreateRequest request) {
    StoredCodefConnection connection = repository.findConnectionByUserId(userId).orElse(null);
    if (connection == null) {
      String demoConnectedId = DEMO_CONNECTION_PREFIX + userId;
      repository.saveConnection(userId, cipher.encrypt(demoConnectedId), hasher.hash(demoConnectedId));
      connection =
          repository
              .findConnectionByUserId(userId)
              .orElseThrow(() -> new IllegalStateException("시연용 계좌 연결 저장에 실패했습니다."));
    }

    long connectionId = connection.connectionId();
    if (MIRAE_ASSET_CODE.equals(request.getOrganizationCode())) {
      ensureSecuritiesAccount(connectionId);
    } else if (KOOKMIN_CODE.equals(request.getOrganizationCode())) {
      ensureDemandAccount(
          connectionId,
          userId,
          KOOKMIN_CODE,
          "KB국민은행",
          "KB나라사랑우대통장",
          "DEMO-KB-1122",
          "****-1122",
          "NARASARANG",
          779_600L);
    } else {
      ensureDemandAccount(
          connectionId,
          userId,
          HANA_CODE,
          "하나은행",
          "나라사랑 하나통장",
          "DEMO-HANA-5678",
          "****-5678",
          "GENERAL",
          468_000L);
    }
    if (BANK_BUSINESS_TYPE.equals(request.getBusinessType())) {
      ensureSoldierSaving(connectionId, userId, request.getOrganizationCode());
    }
    // 실제 CODEF 연결이 남아 있어도 시연 중에는 배치·새로고침 대상에서 제외합니다.
    repository.markConnectionAsDemo(connectionId);
    int syncedAccountCount = BANK_BUSINESS_TYPE.equals(request.getBusinessType()) ? 2 : 1;
    return new CodefConnectionResponse(
        userId, request.getOrganizationCode(), syncedAccountCount, List.of(), List.of());
  }

  private void ensureDemandAccount(
      long connectionId,
      long userId,
      String institutionCode,
      String institutionName,
      String productName,
      String accountNumber,
      String maskedAccountNumber,
      String accountRole,
      long balance) {
    long accountId =
        ensureAccount(
            connectionId,
            institutionCode,
            institutionName,
            accountNumber,
            maskedAccountNumber,
            BANK_BUSINESS_TYPE,
            "DEMAND_DEPOSIT",
            productName,
            balance,
            balance,
            null);
    repository.updateAccountRole(accountId, accountRole);
    LocalDate today = LocalDate.now();
    // 시연 소비 흐름은 월급이 들어오는 국민 입출금 계좌에만 한 번 생성한다.
    // 하나 입출금 계좌에도 같은 거래를 넣으면 통합 거래내역에서 중복으로 보인다.
    if (KOOKMIN_CODE.equals(institutionCode)) {
      saveDemoDemandTransactions(accountId, balance, today);
    }
    repository.recordTransactionSyncPeriod(
        accountId, "DEMAND_DEPOSIT", today.minusMonths(3), today);
  }

  private void saveDemoDemandTransactions(long accountId, long balance, LocalDate today) {
    LocalDate june = today.minusMonths(2);
    LocalDate july = today.minusMonths(1);
    LocalDate august = today;
    List<DemoTransaction> transactions =
        List.of(
            new DemoTransaction(
                june.withDayOfMonth(10).atTime(9, 0),
                750_000L,
                "DEPOSIT",
                "SALARY",
                "오픈뱅킹입금·국군재정단",
                "salary-202606"),
            new DemoTransaction(
                july.withDayOfMonth(10).atTime(9, 0),
                900_000L,
                "DEPOSIT",
                "SALARY",
                "오픈뱅킹입금·국군재정단",
                "salary-202607"),
            new DemoTransaction(
                august.withDayOfMonth(10).atTime(9, 0),
                900_000L,
                "DEPOSIT",
                "SALARY",
                "오픈뱅킹입금·국군재정단",
                "salary-202608"),
            new DemoTransaction(june.withDayOfMonth(3).atTime(8, 0), 14_900L, "WITHDRAW", "LEISURE", "체크카드·YouTube Premium", "youtube-202606"),
            new DemoTransaction(july.withDayOfMonth(3).atTime(8, 0), 14_900L, "WITHDRAW", "LEISURE", "체크카드·YouTube Premium", "youtube-202607"),
            new DemoTransaction(august.withDayOfMonth(3).atTime(8, 0), 14_900L, "WITHDRAW", "LEISURE", "체크카드·YouTube Premium", "youtube-202608"),
            new DemoTransaction(june.withDayOfMonth(7).atTime(8, 0), 9_500L, "WITHDRAW", "LEISURE", "체크카드·티빙 베이직 정기결제", "tving-202606"),
            new DemoTransaction(july.withDayOfMonth(7).atTime(8, 0), 9_500L, "WITHDRAW", "LEISURE", "체크카드·티빙 베이직 정기결제", "tving-202607"),
            new DemoTransaction(august.withDayOfMonth(7).atTime(8, 0), 9_500L, "WITHDRAW", "LEISURE", "체크카드·티빙 베이직 정기결제", "tving-202608"),
            new DemoTransaction(june.withDayOfMonth(11).atTime(9, 5), 300_000L, "WITHDRAW", "ASSET", "장병내일준비적금 자동이체·KB국민은행", "kb-saving-transfer-202606"),
            new DemoTransaction(june.withDayOfMonth(11).atTime(9, 6), 250_000L, "WITHDRAW", "ASSET", "장병내일준비적금 자동이체·하나은행", "hana-saving-transfer-202606"),
            new DemoTransaction(july.withDayOfMonth(11).atTime(9, 5), 300_000L, "WITHDRAW", "ASSET", "장병내일준비적금 자동이체·KB국민은행", "kb-saving-transfer-202607"),
            new DemoTransaction(july.withDayOfMonth(11).atTime(9, 6), 250_000L, "WITHDRAW", "ASSET", "장병내일준비적금 자동이체·하나은행", "hana-saving-transfer-202607"),
            new DemoTransaction(august.withDayOfMonth(11).atTime(9, 5), 300_000L, "WITHDRAW", "ASSET", "장병내일준비적금 자동이체·KB국민은행", "kb-saving-transfer-202608"),
            new DemoTransaction(august.withDayOfMonth(11).atTime(9, 6), 250_000L, "WITHDRAW", "ASSET", "장병내일준비적금 자동이체·하나은행", "hana-saving-transfer-202608"),
            new DemoTransaction(june.withDayOfMonth(14).atTime(18, 20), 6_800L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260614"),
            new DemoTransaction(june.withDayOfMonth(23).atTime(19, 15), 5_300L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260623"),
            new DemoTransaction(july.withDayOfMonth(6).atTime(20, 10), 8_100L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260706"),
            new DemoTransaction(july.withDayOfMonth(17).atTime(17, 40), 5_300L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260717"),
            new DemoTransaction(july.withDayOfMonth(28).atTime(18, 50), 7_900L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260728"),
            new DemoTransaction(august.withDayOfMonth(4).atTime(19, 20), 4_700L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260804"),
            new DemoTransaction(august.withDayOfMonth(16).atTime(14, 20), 89_900L, "WITHDRAW", "SHOPPING", "체크카드·무신사 스탠다드 의류", "musinsa-20260816"),
            new DemoTransaction(august.withDayOfMonth(9).atTime(14, 6), 59_800L, "WITHDRAW", "TRANSPORT", "체크카드·코레일톡 KTX 왕복 승차권", "ktx-20260809"),
            new DemoTransaction(august.withDayOfMonth(22).atTime(20, 30), 7_600L, "WITHDRAW", "PX", "체크카드·국군복지단", "px-20260822"),
            new DemoTransaction(august.withDayOfMonth(23).atTime(18, 26), 15_400L, "WITHDRAW", "FOOD", "체크카드·맥도날드 서울역점", "vacation-mcdonalds"),
            new DemoTransaction(august.withDayOfMonth(23).atTime(20, 5), 6_200L, "WITHDRAW", "FOOD", "체크카드·스타벅스 서울역점", "vacation-starbucks"),
            new DemoTransaction(august.withDayOfMonth(24).atTime(13, 10), 24_900L, "WITHDRAW", "BEAUTY", "체크카드·올리브영 홍대점", "vacation-oliveyoung"),
            new DemoTransaction(august.withDayOfMonth(24).atTime(21, 15), 45_000L, "WITHDRAW", "ETC", "전자금융·김민수", "vacation-transfer"));
    for (DemoTransaction transaction : transactions) {
      saveTransaction(
          accountId,
          transaction.transactionAt(),
          transaction.amount(),
          balance,
          transaction.transactionType(),
          transaction.category(),
          transaction.description(),
          transaction.keySuffix());
    }
  }

  private void ensureSoldierSaving(long connectionId, long userId, String institutionCode) {
    LocalDate startDate = LocalDate.now().minusMonths(3).withDayOfMonth(1);
    LocalDate endDate = LocalDate.now().plusMonths(15).withDayOfMonth(1);
    boolean isKookmin = KOOKMIN_CODE.equals(institutionCode);
    String bankName = isKookmin ? "KB국민은행" : "하나은행";
    String accountNumber = isKookmin ? "DEMO-KB-SAVING-3001" : "DEMO-HANA-SAVING-2501";
    String maskedAccountNumber = isKookmin ? "****-3001" : "****-2501";
    long monthlyAmount = isKookmin ? 300_000L : 250_000L;
    long balance = isKookmin ? 900_000L : 750_000L;

    long accountId =
        ensureAccount(
            connectionId,
            institutionCode,
            bankName,
            accountNumber,
            maskedAccountNumber,
            BANK_BUSINESS_TYPE,
            "INSTALLMENT_SAVINGS",
            "장병내일준비적금",
            balance,
            null,
            endDate);
    repository.updateAccountRole(accountId, "SOLDIER_SAVING");
    repository.upsertDemoSoldierSaving(
        userId, accountId, bankName, monthlyAmount, new BigDecimal("5.00"), startDate, endDate);
    for (int monthsAgo = 2; monthsAgo >= 0; monthsAgo--) {
      int installment = 3 - monthsAgo;
      saveTransaction(
          accountId,
          LocalDate.now().minusMonths(monthsAgo).withDayOfMonth(11).atTime(9, 10),
          monthlyAmount,
          monthlyAmount * installment,
          "DEPOSIT",
          "ASSET",
          "장병내일준비적금 " + installment + "회차 납입",
          (isKookmin ? "kb" : "hana") + "-saving-payment-" + installment);
    }
  }

  private record DemoTransaction(
      LocalDateTime transactionAt,
      long amount,
      String transactionType,
      String category,
      String description,
      String keySuffix) {}

  private void ensureSecuritiesAccount(long connectionId) {
    long accountId =
        ensureAccount(
            connectionId,
            MIRAE_ASSET_CODE,
            "미래에셋증권",
            "DEMO-MIRAE-9820",
            "400643-**-****20",
            SECURITIES_BUSINESS_TYPE,
            "SECURITIES",
            "미래에셋증권 위탁계좌",
            0L,
            0L,
            null);
    repository.updateAccountRole(accountId, "GENERAL");
  }

  private long ensureAccount(
      long connectionId,
      String institutionCode,
      String institutionName,
      String accountNumber,
      String maskedAccountNumber,
      String businessType,
      String accountType,
      String productName,
      long currentBalance,
      Long availableBalance,
      LocalDate maturityDate) {
    String hash = hasher.hash(accountNumber);
    repository.upsertAccount(
        connectionId,
        institutionCode,
        businessType,
        institutionName,
        cipher.encrypt(accountNumber),
        hash,
        maskedAccountNumber,
        accountType,
        productName,
        currentBalance,
        availableBalance,
        LocalDate.now().minusMonths(4),
        maturityDate);
    repository.activateAccountByConnectionInstitutionAndAccountHash(connectionId, institutionCode, hash);
    return repository
        .findAccountIdByConnectionInstitutionAndAccountHash(connectionId, institutionCode, hash)
        .orElseThrow(() -> new IllegalStateException("시연용 계좌 저장에 실패했습니다."));
  }

  private void saveTransaction(
      long accountId,
      LocalDateTime transactionAt,
      long amount,
      long balanceAfter,
      String transactionType,
      String category,
      String description,
      String keySuffix) {
    String externalKey = hasher.hash("demo-transaction-" + accountId + "-" + keySuffix);
    repository.insertTransactionIfAbsent(
        accountId, transactionAt, amount, balanceAfter, transactionType, description, externalKey);
    if (category != null) {
      repository.fillTransactionCategoryIfEmpty(accountId, externalKey, category);
    }
  }

  private boolean matchesLoginId(String requestedLoginId, String configuredLoginId) {
    return configuredLoginId != null
        && !configuredLoginId.isBlank()
        && configuredLoginId.equals(requestedLoginId);
  }
}
