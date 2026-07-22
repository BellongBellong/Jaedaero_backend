package com.jaedaero.codef.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.codef.common.CodefApiClient;
import com.jaedaero.codef.connection.CodefAccountClient;
import com.jaedaero.codef.connection.CodefAccountCreateRequest;
import com.jaedaero.codef.connection.CodefAccountCreateResponse;
import com.jaedaero.codef.institution.CodefBankInstitution;
import java.text.NumberFormat;
import java.time.LocalDate;
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

    private static final String ACCOUNT_LIST_PATH = "/v1/kr/bank/p/account/account-list";
    private static final String TRANSACTION_LIST_PATH = "/v1/kr/bank/p/account/transaction-list";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final CodefAccountClient codefAccountClient;
    private final CodefApiClient codefApiClient;

    public CodefDemoService(CodefAccountClient codefAccountClient, CodefApiClient codefApiClient) {
        this.codefAccountClient = codefAccountClient;
        this.codefApiClient = codefApiClient;
    }

    public String connect(CodefDemoLoginForm form) {
        CodefBankInstitution.fromOrganizationCode(form.getOrganizationCode());

        CodefAccountCreateRequest request = new CodefAccountCreateRequest();
        request.setOrganization(form.getOrganizationCode());
        request.setLoginType("1");
        request.setLoginId(form.getLoginId());
        request.setPassword(form.getPassword());
        request.setBirthday(form.getBirthday());

        CodefAccountCreateResponse response = codefAccountClient.createAccount(request);
        if (response.getConnectedId() == null || response.getConnectedId().isBlank()) {
            throw new IllegalStateException("CODEF Connected ID was not issued.");
        }
        return response.getConnectedId();
    }

    public List<CodefDemoAccountCard> getAccountCards(String connectedId, String organizationCode) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("connectedId", connectedId);
        body.put("organization", organizationCode);

        JsonNode data = codefApiClient.post(ACCOUNT_LIST_PATH, body).path("data");
        List<CodefDemoAccountCard> cards = new ArrayList<>();
        addCards(cards, data.path("resDepositTrust"), "예금·적금", true);
        addCards(cards, data.path("resLoan"), "대출", false);
        addCards(cards, data.path("resFund"), "펀드", false);
        addCards(cards, data.path("resForeignCurrency"), "외화", false);
        addCards(cards, data.path("resInsurance"), "보험", false);
        return cards;
    }

    public List<CodefDemoTransaction> getRecentTransactions(
            String connectedId, String organizationCode, String account) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("connectedId", connectedId);
        body.put("organization", organizationCode);
        body.put("account", account);
        body.put("startDate", DATE_FORMAT.format(today.minusMonths(3)));
        body.put("endDate", DATE_FORMAT.format(today));
        body.put("orderBy", "0");
        body.put("inquiryType", "1");

        JsonNode transactionList = codefApiClient.post(TRANSACTION_LIST_PATH, body)
                .path("data")
                .path("resTrHistoryList");
        List<CodefDemoTransaction> transactions = new ArrayList<>();
        if (!transactionList.isArray()) {
            return transactions;
        }

        for (JsonNode transaction : transactionList) {
            String description = firstText(
                    transaction, "resAccountDesc4", "resAccountDesc3", "resAccountDesc2", "tranDesc");
            transactions.add(new CodefDemoTransaction(
                    transaction.path("resAccountTrDate").asText() + " " + transaction.path("resAccountTrTime").asText(),
                    description,
                    formatAmount(transaction.path("resAccountOut").asText()),
                    formatAmount(transaction.path("resAccountIn").asText()),
                    formatAmount(transaction.path("resAfterTranBalance").asText())));
        }
        return transactions;
    }

    private void addCards(
            List<CodefDemoAccountCard> cards, JsonNode accounts, String category, boolean demandDepositPossible) {
        if (!accounts.isArray()) {
            return;
        }

        for (JsonNode account : accounts) {
            String accountDepositType = account.path("resAccountDeposit").asText();
            boolean transactionSupported = demandDepositPossible
                    && ("10".equals(accountDepositType) || "11".equals(accountDepositType));
            cards.add(new CodefDemoAccountCard(
                    account.path("resAccount").asText(),
                    account.path("resAccountDisplay").asText(),
                    account.path("resAccountName").asText(),
                    category,
                    formatAmount(account.path("resAccountBalance").asText()),
                    transactionSupported));
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "거래 상세 없음";
    }

    private String formatAmount(String amount) {
        try {
            return NumberFormat.getNumberInstance(Locale.KOREA).format(Long.parseLong(amount)) + "원";
        } catch (NumberFormatException exception) {
            return "-";
        }
    }
}
