package com.jaedaero.domain.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.domain.codef.client.CodefApiClient;
import com.jaedaero.domain.codef.institution.CodefBusinessType;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredCodefConnection;
import com.jaedaero.domain.codef.persistence.StoredInstitutionSyncTarget;
import com.jaedaero.global.security.SensitiveValueCipher;
import com.jaedaero.global.security.Sha256Hasher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CODEF 계좌 목록 데이터를 로컬 데이터베이스에 동기화합니다. */
@Service
public class CodefAccountSyncService {
  private static final String ACCOUNT_LIST_PATH = "/v1/kr/bank/p/account/account-list";
  // CODEF 증권 전계좌 API는 개인/법인 구분 없이 공통(a) 엔드포인트를 사용한다.
  private static final String SECURITIES_ACCOUNT_LIST_PATH = "/v1/kr/stock/a/account/account-list";
  private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  private final CodefApiClient codefApiClient;
  private final CodefPersistenceRepository repository;
  private final SensitiveValueCipher cipher;
  private final Sha256Hasher hasher;

  public CodefAccountSyncService(
      CodefApiClient codefApiClient,
      CodefPersistenceRepository repository,
      SensitiveValueCipher cipher,
      Sha256Hasher hasher) {
    this.codefApiClient = codefApiClient;
    this.repository = repository;
    this.cipher = cipher;
    this.hasher = hasher;
  }

  @Transactional
  public int syncBankAccounts(long userId, String organizationCode) {
    return syncAccounts(userId, organizationCode, CodefBusinessType.BANK);
  }

  @Transactional
  public int syncAccounts(long userId, String organizationCode, CodefBusinessType businessType) {
    StoredCodefConnection connection =
        repository
            .findConnectionByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("연결된 CODEF 계정이 없습니다."));
    try {
      Map<String, String> body = new LinkedHashMap<>();
      body.put("connectedId", cipher.decrypt(connection.connectedIdEncrypted()));
      body.put("organization", organizationCode);
      JsonNode data =
          codefApiClient
              .postProduct(
                  businessType == CodefBusinessType.SECURITIES
                      ? SECURITIES_ACCOUNT_LIST_PATH
                      : ACCOUNT_LIST_PATH,
                  body)
              .path("data");
      int count =
          businessType == CodefBusinessType.SECURITIES
              ? saveSecuritiesAccounts(connection, organizationCode, data)
              : saveBankAccounts(connection, organizationCode, data);
      repository.updateConnectionSyncSuccess(connection.connectionId());
      repository.updateInstitutionSyncSuccess(
          connection.connectionId(), organizationCode, businessType.getCode());
      return count;
    } catch (RuntimeException exception) {
      repository.updateConnectionSyncError(connection.connectionId(), exception.getMessage());
      repository.updateInstitutionSyncError(
          connection.connectionId(),
          organizationCode,
          businessType.getCode(),
          exception.getMessage());
      throw exception;
    }
  }

  @Transactional
  public int refreshAllAccounts(long userId) {
    List<StoredInstitutionSyncTarget> syncTargets =
        repository.findInstitutionSyncTargetsByUserId(userId);
    if (syncTargets.isEmpty()) {
      throw new IllegalStateException("동기화할 연결 계좌가 없습니다.");
    }
    int count = 0;
    for (StoredInstitutionSyncTarget syncTarget : syncTargets) {
      count +=
          syncAccounts(
              userId,
              syncTarget.institutionCode(),
              CodefBusinessType.fromCode(syncTarget.businessType()));
    }
    return count;
  }

  private int saveBankAccounts(
      StoredCodefConnection connection, String organizationCode, JsonNode data) {
    int count = 0;
    count += saveAccounts(connection, organizationCode, data.path("resDepositTrust"), "예금·적금");
    count += saveAccounts(connection, organizationCode, data.path("resLoan"), "대출");
    count += saveAccounts(connection, organizationCode, data.path("resFund"), "펀드");
    count += saveAccounts(connection, organizationCode, data.path("resForeignCurrency"), "외화");
    count += saveAccounts(connection, organizationCode, data.path("resInsurance"), "보험");
    return count;
  }

  private int saveSecuritiesAccounts(
      StoredCodefConnection connection, String organizationCode, JsonNode data) {
    // CODEF는 기관에 따라 단일 계좌는 객체로, 복수 계좌는 data 배열로 반환한다.
    JsonNode accounts =
        data.isArray()
            ? data
            : firstArray(data, "resAccountList", "resAccount", "resAccountInfoList");
    if (!accounts.isArray()
        && data.isObject()
        && !firstText(data, "resAccount", "resAccountNo", "resAccountNumber").isBlank()) {
      accounts = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode().add(data);
    }
    if (!accounts.isArray()) {
      return 0;
    }
    int count = 0;
    for (JsonNode account : accounts) {
      String accountNumber = firstText(account, "resAccount", "resAccountNo", "resAccountNumber");
      if (accountNumber.isBlank()) {
        continue;
      }
      String productName =
          firstText(account, "resAccountName", "resAccountProductName", "resAccountTypeName");
      String masked = firstText(account, "resAccountDisplay", "resAccountNoDisplay");
      String institutionName =
          firstText(account, "resAccountBankName", "resCompanyName", "resOrganizationName");
      repository.upsertAccount(
          connection.connectionId(),
          organizationCode,
          CodefBusinessType.SECURITIES.getCode(),
          institutionName.isBlank() ? organizationCode : institutionName,
          cipher.encrypt(accountNumber),
          hasher.hash(accountNumber),
          masked.isBlank() ? mask(accountNumber) : masked,
          "SECURITIES",
          productName.isBlank() ? "증권 계좌" : productName,
          number(
              firstNode(
                  account,
                  "resAccountBalance",
                  "resTotalBalance",
                  "resTotalAsset",
                  "resValuationAmt")),
          nullableNumber(
              firstNode(
                  account, "resAvailableBalance", "resOrderPossibleAmount", "resDepositReceived")),
          parseDate(firstText(account, "resAccountOpenDate", "resAccountStartDate")),
          null);
      count++;
    }
    return count;
  }

  private int saveAccounts(
      StoredCodefConnection connection,
      String organizationCode,
      JsonNode accounts,
      String fallbackType) {
    if (!accounts.isArray()) return 0;
    int count = 0;
    for (JsonNode account : accounts) {
      String accountNumber = account.path("resAccount").asText();
      if (accountNumber.isBlank()) continue;
      String productName = account.path("resAccountName").asText();
      String masked = textOrDefault(account, "resAccountDisplay", mask(accountNumber));
      String institutionName = textOrDefault(account, "resAccountBankName", organizationCode);
      String accountType = resolveAccountType(account, fallbackType);
      repository.upsertAccount(
          connection.connectionId(),
          organizationCode,
          CodefBusinessType.BANK.getCode(),
          institutionName,
          cipher.encrypt(accountNumber),
          hasher.hash(accountNumber),
          masked,
          accountType,
          productName,
          number(account.path("resAccountBalance")),
          nullableNumber(account.path("resAccountAvailBalance")),
          parseDate(account.path("resAccountOpenDate").asText()),
          parseDate(account.path("resAccountEndDate").asText()));
      count++;
    }
    return count;
  }

  private String resolveAccountType(JsonNode account, String fallbackType) {
    String depositCode = account.path("resAccountDeposit").asText();
    if ("12".equals(depositCode)) return "INSTALLMENT_SAVINGS";
    if ("10".equals(depositCode) || "11".equals(depositCode)) return "DEMAND_DEPOSIT";
    return fallbackType;
  }

  private long number(JsonNode node) {
    Long value = nullableNumber(node);
    return value == null ? 0L : value;
  }

  private Long nullableNumber(JsonNode node) {
    String value = node.asText().replaceAll("[^0-9-]", "");
    if (value.isBlank() || "-".equals(value)) return null;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private JsonNode firstArray(JsonNode node, String... fields) {
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (value.isArray()) {
        return value;
      }
    }
    return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
  }

  private JsonNode firstNode(JsonNode node, String... fields) {
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
        return value;
      }
    }
    return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
  }

  private String firstText(JsonNode node, String... fields) {
    return firstNode(node, fields).asText();
  }

  private LocalDate parseDate(String value) {
    if (value == null || !value.matches("\\d{8}")) return null;
    try {
      return LocalDate.parse(value, BASIC_DATE);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private String textOrDefault(JsonNode node, String field, String fallback) {
    String value = node.path(field).asText();
    return value.isBlank() ? fallback : value;
  }

  private String mask(String accountNumber) {
    if (accountNumber.length() <= 4) return "****";
    return "***-***-" + accountNumber.substring(accountNumber.length() - 4);
  }
}
