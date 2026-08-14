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

/** CODEF 증권 계좌 입출금내역을 공통 거래내역 저장소에 동기화합니다. */
@Service
public class CodefSecuritiesCashTransactionSyncService {
  private static final String CASH_TRANSACTION_LIST_PATH = "/v1/kr/stock/a/account/transaction-list";
  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final CodefApiClient codefApiClient;
  private final CodefPersistenceRepository repository;
  private final SensitiveValueCipher cipher;
  private final Sha256Hasher hasher;

  public CodefSecuritiesCashTransactionSyncService(
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
    if (!"ST".equals(account.businessType())) {
      throw new IllegalArgumentException("증권 계좌에 대해서만 조회할 수 있습니다.");
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
            .postProduct(CASH_TRANSACTION_LIST_PATH, body)
            .path("data")
            .path("resTrHistoryList");
    if (!rows.isArray()) return 0;

    int count = 0;
    for (JsonNode row : rows) {
      long income = number(row.path("resAccountIn"));
      long outcome = number(row.path("resAccountOut"));
      if (income == 0 && outcome == 0) continue;
      String date = row.path("resAccountTrDate").asText();
      String time = String.format("%6s", row.path("resAccountTrTime").asText()).replace(' ', '0');
      if (!date.matches("\\d{8}") || !time.matches("\\d{6}")) continue;
      String type = income > 0 ? "DEPOSIT" : "WITHDRAW";
      long amount = income > 0 ? income : outcome;
      String description = description(row);
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
                  + description);
      repository.insertTransactionIfAbsent(
          accountId,
          LocalDateTime.parse(date + time, DATE_TIME_FORMAT),
          amount,
          nullableNumber(row.path("resAfterTranBalance")),
          type,
          description,
          key);
      if (isStockTradeSettlement(description)) {
        repository.fillTransactionCategoryIfEmpty(accountId, key, TransactionCategory.ASSET.name());
      }
      count++;
    }
    return count;
  }

  private String description(JsonNode row) {
    String first = row.path("resAccountDesc2").asText().trim();
    String second = row.path("resAccountDesc3").asText().trim();
    String third = row.path("resAccountDesc4").asText().trim();
    if (!first.isBlank() && !second.isBlank() && !first.equals(second)) return first + " · " + second;
    if (!first.isBlank()) return first;
    if (!second.isBlank()) return second;
    return third.isBlank() ? "증권 계좌 입출금" : third;
  }

  private boolean isStockTradeSettlement(String description) {
    String normalized = description.replaceAll("[\\s\\p{Punct}]", "");
    return normalized.contains("주식매도입금") || normalized.contains("주식매수출금");
  }

  private long number(JsonNode node) {
    String value = node.asText("").replaceAll("[^0-9-]", "");
    try {
      return value.isBlank() || "-".equals(value) ? 0L : Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return 0L;
    }
  }

  private Long nullableNumber(JsonNode node) {
    String value = node.asText("").replaceAll("[^0-9-]", "");
    try {
      return value.isBlank() || "-".equals(value) ? null : Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
