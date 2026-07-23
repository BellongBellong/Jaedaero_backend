package com.jaedaero.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.codef.common.CodefApiClient;
import com.jaedaero.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.codef.persistence.StoredCodefConnection;
import com.jaedaero.codef.persistence.StoredConnectedAccount;
import com.jaedaero.codef.security.SensitiveValueCipher;
import com.jaedaero.codef.security.Sha256Hasher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Synchronizes CODEF installment-savings transactions and military-savings details. */
@Service
public class CodefSavingsTransactionSyncService {

    private static final String INSTALLMENT_SAVINGS_TRANSACTION_LIST_PATH =
            "/v1/kr/bank/p/installment-savings/transaction-list";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final CodefApiClient codefApiClient;
    private final CodefPersistenceRepository repository;
    private final SensitiveValueCipher cipher;
    private final Sha256Hasher hasher;

    public CodefSavingsTransactionSyncService(
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
        validatePeriod(startDate, endDate);
        StoredConnectedAccount account = repository.findAccountByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자의 연동 계좌를 찾을 수 없습니다."));
        if (!"INSTALLMENT_SAVINGS".equals(account.accountType())) {
            throw new IllegalArgumentException("적금 거래내역은 적금 계좌에 대해서만 조회할 수 있습니다.");
        }
        StoredCodefConnection connection = repository.findConnectionByUserId(userId)
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

        JsonNode data = codefApiClient.post(INSTALLMENT_SAVINGS_TRANSACTION_LIST_PATH, body).path("data");
        syncSoldierSavingIfMatched(userId, account, data);

        JsonNode rows = data.path("resTrHistoryList");
        if (!rows.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode row : rows) {
            long income = number(row.path("resAccountIn"));
            long outcome = number(row.path("resAccountOut"));
            String type = income > 0 ? "DEPOSIT" : "WITHDRAW";
            long amount = income > 0 ? income : outcome;
            String transactionDate = row.path("resAccountTrDate").asText();
            LocalDateTime transactionAt = parseTransactionDate(transactionDate);
            String description = description(row);
            String roundNo = row.path("resRoundNo").asText();
            String legacyKeyDescription = firstText(
                    row, "resAccountDesc4", "resAccountDesc3", "resAccountDesc2", "resAccountDesc1");
            String key = hasher.hash(accountNumber + "|" + transactionDate + "|" + roundNo + "|"
                    + income + "|" + outcome + "|" + row.path("resAfterTranBalance").asText()
                    + "|" + legacyKeyDescription);
            repository.insertTransactionIfAbsent(
                    accountId,
                    transactionAt,
                    amount,
                    nullableNumber(row.path("resAfterTranBalance")),
                    type,
                    description,
                    key);
            count++;
        }
        return count;
    }

    private void syncSoldierSavingIfMatched(long userId, StoredConnectedAccount account, JsonNode data) {
        String productName = data.path("resAccountName").asText(account.productName());
        if (!productName.replaceAll("\\s", "").contains("장병내일준비적금")) {
            return;
        }
        repository.upsertSoldierSaving(
                userId,
                account.accountId(),
                account.institutionName(),
                nullableNumber(data.path("resMonthlyPayment")),
                nullableDecimal(data.path("resRate")),
                parseDate(data.path("resAccountStartDate").asText()),
                parseDate(data.path("resAccountEndDate").asText()));
    }

    private void validatePeriod(String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("조회 기간은 yyyyMMdd 형식의 유효한 시작일과 종료일이어야 합니다.");
        }
    }

    private LocalDateTime parseTransactionDate(String value) {
        LocalDate date = parseDate(value);
        if (date == null) {
            throw new IllegalStateException("CODEF 적금 거래일 형식이 올바르지 않습니다.");
        }
        return date.atStartOfDay();
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

    private String description(JsonNode row) {
        String transactionType = firstText(row, "resAccountDesc3", "resAccountDesc2", "resAccountDesc1");
        String branch = firstText(row, "resAccountDesc4");
        if (transactionType.isBlank()) {
            return branch.isBlank() ? "적금 거래" : branch;
        }
        return branch.isBlank() || transactionType.equals(branch)
                ? transactionType
                : transactionType + " · " + branch;
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
        if (value.isBlank() || "-".equals(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal nullableDecimal(JsonNode node) {
        String value = node.asText().trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
