package com.jaedaero.codef.demo;

import com.jaedaero.codef.account.CodefSavingsTransactionSyncService;
import com.jaedaero.codef.account.CodefTransactionSyncService;
import com.jaedaero.codef.connection.CodefBankConnectionCreateRequest;
import com.jaedaero.codef.connection.CodefConnectionService;
import com.jaedaero.codef.institution.CodefBankInstitution;
import com.jaedaero.codef.institution.CodefBusinessType;
import com.jaedaero.codef.institution.CodefSecuritiesInstitution;
import com.jaedaero.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.codef.persistence.StoredConnectedAccount;
import com.jaedaero.codef.persistence.StoredTransaction;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Orchestrates the server-rendered, local-only CODEF account/transaction demonstration. */
@Service
public class CodefDemoService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CodefPersistenceRepository repository;
    private final CodefConnectionService connectionService;
    private final CodefTransactionSyncService transactionSyncService;
    private final CodefSavingsTransactionSyncService savingsTransactionSyncService;

    public CodefDemoService(
            CodefPersistenceRepository repository,
            CodefConnectionService connectionService,
            CodefTransactionSyncService transactionSyncService,
            CodefSavingsTransactionSyncService savingsTransactionSyncService) {
        this.repository = repository;
        this.connectionService = connectionService;
        this.transactionSyncService = transactionSyncService;
        this.savingsTransactionSyncService = savingsTransactionSyncService;
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
        if (!repository.findAccountsByUserIdAndInstitution(
                userId, form.getOrganizationCode(), businessType.getCode()).isEmpty()) {
            return;
        }
        CodefBankConnectionCreateRequest request = new CodefBankConnectionCreateRequest();
        request.setUserId(userId);
        request.setOrganizationCode(form.getOrganizationCode());
        request.setBusinessType(businessType.getCode());
        request.setLoginId(form.getLoginId());
        request.setPassword(form.getPassword());
        request.setBirthDate(form.getBirthDate());
        connectionService.connect(userId, request);
    }

    public CodefDemoAccountOverview getAccountOverview(long userId, String organizationCode, String businessType) {
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
            militarySavings.add(new CodefDemoSavingsCard(
                    account.productName(), account.accountMasked(), formatAmount(account.currentBalance()),
                    maturityDate == null ? "만기일 정보 없음" : maturityDate.toString(), matured));
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
                    CodefDemoSavingsStatus.Result.ACTIVE,
                    "장병내일준비적금 가입 상태가 확인되었습니다.",
                    militarySavings);
        }

        return new CodefDemoSavingsStatus(
                CodefDemoSavingsStatus.Result.MATURED,
                "장병내일준비적금이 모두 만기된 상태입니다.",
                militarySavings);
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
            long userId,
            long accountId,
            String transactionKind,
            LocalDate startDate,
            LocalDate endDate) {
        String inquiryType = "INSTALLMENT_SAVINGS".equals(transactionKind)
                ? "INSTALLMENT_SAVINGS" : "DEMAND_DEPOSIT";
        StoredConnectedAccount account = repository.findAccountByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("연결된 계좌를 찾을 수 없습니다."));
        if (!inquiryType.equals(account.accountType())) {
            throw new IllegalArgumentException("선택한 계좌의 거래 유형이 올바르지 않습니다.");
        }
        if (!repository.isTransactionPeriodCovered(accountId, inquiryType, startDate, endDate)) {
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
        for (StoredTransaction transaction : repository.findTransactions(accountId, startDate, endDate)) {
            boolean deposit = "DEPOSIT".equals(transaction.transactionType());
            transactions.add(new CodefDemoTransaction(
                    DATE_FORMAT.format(transaction.transactionAt().toLocalDate()),
                    TIME_FORMAT.format(transaction.transactionAt().toLocalTime()),
                    transaction.description(),
                    formatAmount(transaction.amount()),
                    transaction.balanceAfter() == null ? "-" : formatAmount(transaction.balanceAfter()),
                    deposit));
        }
        return transactions;
    }

    public String getAccountDisplay(long userId, long accountId) {
        return repository.findAccountByIdAndUserId(accountId, userId)
                .map(StoredConnectedAccount::accountMasked)
                .orElseThrow(() -> new IllegalArgumentException("연결된 계좌를 찾을 수 없습니다."));
    }

    private void addCard(List<CodefDemoAccountCard> cards, StoredConnectedAccount account) {
        boolean demandDeposit = "DEMAND_DEPOSIT".equals(account.accountType());
        boolean installmentSavings = "INSTALLMENT_SAVINGS".equals(account.accountType());
        cards.add(new CodefDemoAccountCard(
                account.accountId(), account.accountMasked(), account.productName(), category(account.accountType()),
                formatAmount(account.currentBalance()), demandDeposit || installmentSavings,
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

    private String formatAmount(long amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
    }
}
