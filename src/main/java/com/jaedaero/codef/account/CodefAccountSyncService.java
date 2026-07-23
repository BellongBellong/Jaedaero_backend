package com.jaedaero.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.codef.common.CodefApiClient;
import com.jaedaero.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.codef.persistence.StoredCodefConnection;
import com.jaedaero.codef.security.SensitiveValueCipher;
import com.jaedaero.codef.security.Sha256Hasher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Synchronizes CODEF account-list data into the local database. */
@Service
public class CodefAccountSyncService {
    private static final String ACCOUNT_LIST_PATH = "/v1/kr/bank/p/account/account-list";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final CodefApiClient codefApiClient;
    private final CodefPersistenceRepository repository;
    private final SensitiveValueCipher cipher;
    private final Sha256Hasher hasher;

    public CodefAccountSyncService(CodefApiClient codefApiClient, CodefPersistenceRepository repository,
            SensitiveValueCipher cipher, Sha256Hasher hasher) {
        this.codefApiClient = codefApiClient;
        this.repository = repository;
        this.cipher = cipher;
        this.hasher = hasher;
    }

    @Transactional
    public int syncBankAccounts(long userId, String organizationCode) {
        StoredCodefConnection connection = repository.findConnectionByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("연결된 CODEF 계정이 없습니다."));
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("connectedId", cipher.decrypt(connection.connectedIdEncrypted()));
            body.put("organization", organizationCode);
            JsonNode data = codefApiClient.post(ACCOUNT_LIST_PATH, body).path("data");
            int count = 0;
            count += saveAccounts(connection, organizationCode, data.path("resDepositTrust"), "예금·적금");
            count += saveAccounts(connection, organizationCode, data.path("resLoan"), "대출");
            count += saveAccounts(connection, organizationCode, data.path("resFund"), "펀드");
            count += saveAccounts(connection, organizationCode, data.path("resForeignCurrency"), "외화");
            count += saveAccounts(connection, organizationCode, data.path("resInsurance"), "보험");
            repository.updateConnectionSyncSuccess(connection.connectionId());
            return count;
        } catch (RuntimeException exception) {
            repository.updateConnectionSyncError(connection.connectionId(), exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public int refreshAllAccounts(long userId) {
        List<String> organizationCodes = repository.findInstitutionCodesByUserId(userId);
        if (organizationCodes.isEmpty()) {
            throw new IllegalStateException("동기화할 연결 계좌가 없습니다.");
        }
        int count = 0;
        for (String organizationCode : organizationCodes) {
            count += syncBankAccounts(userId, organizationCode);
        }
        return count;
    }

    private int saveAccounts(StoredCodefConnection connection, String organizationCode, JsonNode accounts, String fallbackType) {
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
                    connection.connectionId(), organizationCode, institutionName,
                    cipher.encrypt(accountNumber), hasher.hash(accountNumber), masked, accountType,
                    productName, number(account.path("resAccountBalance")), nullableNumber(account.path("resAccountAvailBalance")),
                    parseDate(account.path("resAccountOpenDate").asText()), parseDate(account.path("resAccountEndDate").asText()));
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
        try { return Long.parseLong(value); } catch (NumberFormatException exception) { return null; }
    }

    private LocalDate parseDate(String value) {
        if (value == null || !value.matches("\\d{8}")) return null;
        try { return LocalDate.parse(value, BASIC_DATE); } catch (RuntimeException exception) { return null; }
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
