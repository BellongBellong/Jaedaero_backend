package com.jaedaero.codef.demo;

import com.jaedaero.codef.account.CodefSavingsTransactionSyncService;
import com.jaedaero.codef.account.CodefAccountSyncService;
import com.jaedaero.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.codef.account.CodefTransactionSyncService;
import com.jaedaero.codef.account.SecuritiesAssetResponse;
import com.jaedaero.codef.account.SecuritiesHoldingResponse;
import com.jaedaero.codef.connection.CodefBankConnectionCreateRequest;
import com.jaedaero.codef.connection.CodefConnectionService;
import com.jaedaero.codef.institution.CodefBankInstitution;
import com.jaedaero.codef.institution.CodefBusinessType;
import com.jaedaero.codef.institution.CodefSecuritiesInstitution;
import com.jaedaero.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.codef.persistence.StoredConnectedAccount;
import com.jaedaero.codef.persistence.StoredInstitutionConnection;
import com.jaedaero.codef.persistence.StoredTransaction;
import com.jaedaero.codef.security.SensitiveValueCipher;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final CodefSecuritiesInquiryService securitiesInquiryService;
    private final CodefAccountSyncService accountSyncService;
    private final SensitiveValueCipher cipher;

    public CodefDemoService(
            CodefPersistenceRepository repository,
            CodefConnectionService connectionService,
            CodefTransactionSyncService transactionSyncService,
            CodefSavingsTransactionSyncService savingsTransactionSyncService,
            CodefSecuritiesInquiryService securitiesInquiryService,
            CodefAccountSyncService accountSyncService,
            SensitiveValueCipher cipher) {
        this.repository = repository;
        this.connectionService = connectionService;
        this.transactionSyncService = transactionSyncService;
        this.savingsTransactionSyncService = savingsTransactionSyncService;
        this.securitiesInquiryService = securitiesInquiryService;
        this.accountSyncService = accountSyncService;
        this.cipher = cipher;
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
        List<CodefDemoSavedInstitution> savedInstitutions = new ArrayList<>();
        for (StoredInstitutionConnection connection : repository.findActiveInstitutionConnectionsByUserId(userId)) {
            savedInstitutions.add(new CodefDemoSavedInstitution(
                    connection.institutionCode(), connection.businessType(),
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
                    SecuritiesAssetResponse response = securitiesInquiryService.getFinancialAssets(userId, account.accountId());
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
            boolean bankDetailAvailable = "DEMAND_DEPOSIT".equals(account.accountType())
                    || "INSTALLMENT_SAVINGS".equals(account.accountType());
            String transactionKind = "INSTALLMENT_SAVINGS".equals(account.accountType())
                    ? "INSTALLMENT_SAVINGS" : "DEMAND_DEPOSIT";
            String institutionName = institutionName(account.institutionCode(), account.businessType());
            itemsByInstitution.computeIfAbsent(institutionName, ignored -> new ArrayList<>())
                    .add(new CodefDemoUnifiedAccountAsset(
                            account.accountId(), institutionName, account.productName(), account.accountMasked(),
                            formatAmount(amount), statusMessage, securities, bankDetailAvailable, transactionKind));
        }
        List<CodefDemoInstitutionAssetGroup> institutionGroups = new ArrayList<>();
        for (Map.Entry<String, List<CodefDemoUnifiedAccountAsset>> entry : itemsByInstitution.entrySet()) {
            institutionGroups.add(new CodefDemoInstitutionAssetGroup(entry.getKey(), entry.getValue()));
        }
        return new CodefDemoUnifiedAssets(formatAmount(totalAmount), refreshedAccountCount, institutionGroups);
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

    public CodefDemoSecuritiesPortfolio getSecuritiesAssets(long userId, long accountId) {
        return toPortfolio(securitiesInquiryService.getFinancialAssets(userId, accountId));
    }

    public CodefDemoSecuritiesPortfolio getStockHoldings(long userId, long accountId) {
        return toPortfolio(securitiesInquiryService.getStockHoldings(userId, accountId));
    }

    private CodefDemoSecuritiesPortfolio toPortfolio(SecuritiesAssetResponse response) {
        List<CodefDemoSecuritiesHolding> holdings = new ArrayList<>();
        for (SecuritiesHoldingResponse holding : response.holdings()) {
            holdings.add(new CodefDemoSecuritiesHolding(
                    holding.productType(), holding.itemName(), holding.itemCode(), emptyAsDash(holding.quantity()),
                    formatAmount(holding.purchaseAmount()), formatAmount(holding.valuationAmount()),
                    formatSignedAmount(holding.valuationProfit()), emptyAsDash(holding.earningsRate()),
                    emptyAsDash(holding.currency())));
        }
        return new CodefDemoSecuritiesPortfolio(
                response.accountMasked(), formatAmount(response.depositAmount()), holdings);
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
