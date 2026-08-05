package com.jaedaero.domain.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.domain.codef.client.CodefApiClient;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredCodefConnection;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import com.jaedaero.global.security.SensitiveValueCipher;
import com.jaedaero.global.security.Sha256Hasher;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodefTransactionSyncService {
  private static final String TRANSACTION_LIST_PATH = "/v1/kr/bank/p/account/transaction-list";
  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final CodefApiClient codefApiClient;
  private final CodefPersistenceRepository repository;
  private final SensitiveValueCipher cipher;
  private final Sha256Hasher hasher;

  public CodefTransactionSyncService(
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
  public int sync(long userId, long accountId, String startDate, String endDate) {
    StoredConnectedAccount account =
        repository
            .findAccountByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자의 연동 계좌를 찾을 수 없습니다."));
    if (!"DEMAND_DEPOSIT".equals(account.accountType())) {
      throw new IllegalArgumentException("입출금 거래내역은 입출금 계좌에 대해서만 조회할 수 있습니다.");
    }
    StoredCodefConnection connection =
        repository
            .findConnectionByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("연결된 CODEF 계정이 없습니다."));
    String accountNumber = cipher.decrypt(account.accountNumberEncrypted());
    Map<String, String> body = new LinkedHashMap<>();
    body.put("connectedId", cipher.decrypt(connection.connectedIdEncrypted()));
    body.put("organization", account.institutionCode());
    body.put("account", accountNumber);
    body.put("startDate", startDate);
    body.put("endDate", endDate);
    body.put("orderBy", "0");
    body.put("inquiryType", "1");

    JsonNode rows =
        codefApiClient
            .postProduct(TRANSACTION_LIST_PATH, body)
            .path("data")
            .path("resTrHistoryList");
    if (!rows.isArray()) return 0;
    int count = 0;
    for (JsonNode row : rows) {
      long income = number(row.path("resAccountIn"));
      long outcome = number(row.path("resAccountOut"));
      String type = income > 0 ? "DEPOSIT" : "WITHDRAW";
      long amount = income > 0 ? income : outcome;
      String date = row.path("resAccountTrDate").asText();
      String time = String.format("%6s", row.path("resAccountTrTime").asText()).replace(' ', '0');
      LocalDateTime transactionAt = LocalDateTime.parse(date + time, DATE_TIME_FORMAT);
      String description = description(row);
      String legacyKeyDescription =
          firstText(row, "resAccountDesc4", "resAccountDesc3", "resAccountDesc2", "tranDesc");
      String key =
          hasher.hash(
              accountNumber
                  + "|"
                  + date
                  + "|"
                  + time
                  + "|"
                  + income
                  + "|"
                  + outcome
                  + "|"
                  + row.path("resAfterTranBalance").asText()
                  + "|"
                  + legacyKeyDescription);
      repository.insertTransactionIfAbsent(
          accountId,
          transactionAt,
          amount,
          nullableNumber(row.path("resAfterTranBalance")),
          type,
          description,
          key);
      if ("WITHDRAW".equals(type)) {
        repository.fillTransactionCategoryIfEmpty(
            accountId, key, TransactionCategory.fromDescription(description).name());
      }
      count++;
    }
    return count;
  }

  private String description(JsonNode row) {
    String transactionType = firstText(row, "resAccountDesc2", "resAccountDesc3", "tranDesc");
    String detail = firstText(row, "resAccountDesc3", "tranDesc", "resAccountDesc4");
    if (transactionType.isBlank()) {
      return detail.isBlank() ? "거래 상세 없음" : detail;
    }
    return detail.isBlank() || transactionType.equals(detail)
        ? transactionType
        : transactionType + " · " + detail;
  }

  private String firstText(JsonNode row, String... fields) {
    for (String field : fields) {
      String value = row.path(field).asText().trim();
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
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
}
