package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.connection.CodefConnectionService;
import com.jaedaero.domain.codef.institution.CodefBusinessType;
import com.jaedaero.domain.codef.institution.CodefSecuritiesInstitution;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiOperation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "CODEF 금융 계좌·거래내역")
@RequestMapping("/api/v1/codef")
public class CodefAccountController {
    private final CodefPersistenceRepository repository;
    private final CodefAccountSyncService syncService;
    private final CodefTransactionSyncService transactionSyncService;
    private final CodefSavingsTransactionSyncService savingsTransactionSyncService;
    private final CodefConnectionService connectionService;
    private final CodefSecuritiesInquiryService securitiesInquiryService;

    public CodefAccountController(
            CodefPersistenceRepository repository,
            CodefAccountSyncService syncService,
            CodefTransactionSyncService transactionSyncService,
            CodefSavingsTransactionSyncService savingsTransactionSyncService,
            CodefConnectionService connectionService,
            CodefSecuritiesInquiryService securitiesInquiryService) {
        this.repository = repository;
        this.syncService = syncService;
        this.transactionSyncService = transactionSyncService;
        this.savingsTransactionSyncService = savingsTransactionSyncService;
        this.connectionService = connectionService;
        this.securitiesInquiryService = securitiesInquiryService;
    }

    @ApiOperation(
            value = "연결된 전체 계좌 조회",
            notes = "사용자의 은행·증권 연동 계좌를 조회합니다. refresh를 true로 설정하면 CODEF에서 최신 계좌 정보를 다시 가져옵니다.")
    @GetMapping("/accounts")
    public List<ConnectedAccountResponse> getAccounts(
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId,
            @ApiParam(value = "CODEF에서 최신 계좌 정보를 다시 조회할지 여부", example = "false")
                    @RequestParam(defaultValue = "false") boolean refresh) {
        if (refresh) syncService.refreshAllAccounts(userId);
        return repository.findAccountsByUserId(userId).stream()
                .map(ConnectedAccountResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "연결된 적금 계좌 조회",
            notes = "사용자의 연동 계좌 중 적금 계좌만 조회합니다. refresh를 true로 설정하면 CODEF에서 최신 정보를 다시 가져옵니다.")
    @GetMapping("/savings")
    public List<ConnectedAccountResponse> getSavings(
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId,
            @ApiParam(value = "CODEF에서 최신 계좌 정보를 다시 조회할지 여부", example = "false")
                    @RequestParam(defaultValue = "false") boolean refresh) {
        if (refresh) syncService.refreshAllAccounts(userId);
        return repository.findSavingsByUserId(userId).stream()
                .map(ConnectedAccountResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "연결된 증권 계좌 조회", notes = "사용자의 연동 계좌 중 증권사 계좌만 조회합니다.")
    @GetMapping("/securities/accounts")
    public List<ConnectedAccountResponse> getSecuritiesAccounts(
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId) {
        return repository.findAccountsByUserId(userId).stream()
                .filter(account -> "SECURITIES".equals(account.accountType()))
                .map(ConnectedAccountResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "증권 계좌 종합자산 조회",
            notes = "증권 계좌의 예수금과 주식·펀드 등 상품별 평가 정보를 CODEF에서 실시간으로 조회합니다.")
    @GetMapping("/securities/accounts/{accountId}/assets")
    public SecuritiesAssetResponse getSecuritiesAssets(
            @ApiParam(value = "증권 계좌 ID", required = true, example = "10")
                    @org.springframework.web.bind.annotation.PathVariable long accountId,
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId) {
        return securitiesInquiryService.getFinancialAssets(userId, accountId);
    }

    @ApiOperation(
            value = "증권 계좌 주식 잔고 조회",
            notes = "증권 계좌에서 보유 중인 주식 종목의 수량, 평가금액, 평가손익을 CODEF에서 실시간으로 조회합니다.")
    @GetMapping("/securities/accounts/{accountId}/holdings")
    public SecuritiesAssetResponse getSecuritiesHoldings(
            @ApiParam(value = "증권 계좌 ID", required = true, example = "10")
                    @org.springframework.web.bind.annotation.PathVariable long accountId,
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId) {
        return securitiesInquiryService.getStockHoldings(userId, accountId);
    }

    @ApiOperation(
            value = "증권사 등록 여부 조회",
            notes = "DB에 저장된 연결 상태만 조회하며 CODEF를 다시 호출하지 않습니다. Connected ID는 응답에 노출하지 않습니다.")
    @GetMapping("/securities/registration-status")
    public SecuritiesRegistrationStatusResponse getSecuritiesRegistrationStatus(
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId,
            @ApiParam(value = "CODEF 증권사 기관 코드", required = true, example = "0238")
                    @RequestParam String organizationCode) {
        CodefSecuritiesInstitution institution =
                CodefSecuritiesInstitution.fromOrganizationCode(organizationCode);
        int accountCount = repository
                .findAccountsByUserIdAndInstitution(
                        userId, institution.getOrganizationCode(), CodefBusinessType.SECURITIES.getCode())
                .size();
        boolean registered = repository
                .findInstitutionConnection(userId, institution.getOrganizationCode(), CodefBusinessType.SECURITIES.getCode())
                .filter(connection -> "ACTIVE".equals(connection.status()))
                .isPresent();
        return new SecuritiesRegistrationStatusResponse(
                userId,
                institution.getOrganizationCode(),
                institution.getDisplayName(),
                repository.findConnectionByUserId(userId).isPresent(),
                registered,
                accountCount);
    }

    @ApiOperation(
            value = "연결된 기관 계좌 재동기화",
            notes = "이미 CODEF에 등록된 은행(BK) 또는 증권사(ST)의 계좌 목록만 다시 조회합니다. 로그인 정보와 Connected ID를 다시 받지 않습니다.")
    @PostMapping("/institutions/{businessType}/{organizationCode}/sync")
    public List<ConnectedAccountResponse> syncInstitutionAccounts(
            @ApiParam(value = "BK: 은행, ST: 증권", required = true, example = "ST")
                    @org.springframework.web.bind.annotation.PathVariable String businessType,
            @ApiParam(value = "CODEF 기관 코드", required = true, example = "0238")
                    @org.springframework.web.bind.annotation.PathVariable String organizationCode,
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId) {
        connectionService.syncRegisteredInstitution(userId, organizationCode, businessType);
        return repository.findAccountsByUserIdAndInstitution(userId, organizationCode, businessType).stream()
                .map(ConnectedAccountResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "적금 거래내역 조회",
            notes = "기본 최근 3개월을 저장하며, 요청 기간이 DB 동기화 범위를 벗어난 경우에만 CODEF를 다시 호출합니다.")
    @GetMapping("/savings/{accountId}/transactions")
    public List<TransactionResponse> getSavingsTransactions(
            @ApiParam(value = "적금 계좌 ID", required = true, example = "10")
                    @org.springframework.web.bind.annotation.PathVariable long accountId,
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId,
            @ApiParam(value = "조회 시작일(yyyyMMdd). 생략하면 종료일 기준 3개월 전입니다.", example = "20260401")
                    @RequestParam(required = false) String startDate,
            @ApiParam(value = "조회 종료일(yyyyMMdd). 생략하면 오늘입니다.", example = "20260729")
                    @RequestParam(required = false) String endDate,
            @ApiParam(value = "저장된 조회 결과와 관계없이 CODEF에서 다시 조회할지 여부", example = "false")
                    @RequestParam(defaultValue = "false") boolean refresh) {
        repository.findAccountByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자의 연동 계좌를 찾을 수 없습니다."));
        InquiryPeriod period = inquiryPeriod(startDate, endDate);
        if (refresh || !repository.isTransactionPeriodCovered(
                accountId, "INSTALLMENT_SAVINGS", period.startDate(), period.endDate())) {
            savingsTransactionSyncService.sync(userId, accountId, period.startDateText(), period.endDateText());
            repository.recordTransactionSyncPeriod(
                    accountId, "INSTALLMENT_SAVINGS", period.startDate(), period.endDate());
        }
        return repository.findTransactions(accountId, period.startDate(), period.endDate()).stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "입출금 계좌 거래내역 조회",
            notes = "입출금 계좌의 거래내역을 조회합니다. 기본 조회 기간은 최근 3개월이며, 저장된 범위 밖의 기간 또는 refresh=true 요청만 CODEF를 다시 호출합니다.")
    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> getTransactions(
            @ApiParam(value = "입출금 계좌 ID", required = true, example = "10")
                    @org.springframework.web.bind.annotation.PathVariable long accountId,
            @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId,
            @ApiParam(value = "조회 시작일(yyyyMMdd). 생략하면 종료일 기준 3개월 전입니다.", example = "20260401")
                    @RequestParam(required = false) String startDate,
            @ApiParam(value = "조회 종료일(yyyyMMdd). 생략하면 오늘입니다.", example = "20260729")
                    @RequestParam(required = false) String endDate,
            @ApiParam(value = "저장된 조회 결과와 관계없이 CODEF에서 다시 조회할지 여부", example = "false")
                    @RequestParam(defaultValue = "false") boolean refresh) {
        repository.findAccountByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자의 연동 계좌를 찾을 수 없습니다."));
        InquiryPeriod period = inquiryPeriod(startDate, endDate);
        if (refresh || !repository.isTransactionPeriodCovered(
                accountId, "DEMAND_DEPOSIT", period.startDate(), period.endDate())) {
            transactionSyncService.sync(userId, accountId, period.startDateText(), period.endDateText());
            repository.recordTransactionSyncPeriod(
                    accountId, "DEMAND_DEPOSIT", period.startDate(), period.endDate());
        }
        return repository.findTransactions(accountId, period.startDate(), period.endDate()).stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    private InquiryPeriod inquiryPeriod(String requestedStartDate, String requestedEndDate) {
        LocalDate endDate = requestedEndDate == null || requestedEndDate.isBlank()
                ? LocalDate.now()
                : parseDate(requestedEndDate);
        LocalDate startDate = requestedStartDate == null || requestedStartDate.isBlank()
                ? endDate.minusMonths(3)
                : parseDate(requestedStartDate);
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        return new InquiryPeriod(startDate, endDate);
    }

    private LocalDate parseDate(String dateText) {
        if (!dateText.matches("\\d{8}")) {
            throw new IllegalArgumentException("조회 날짜는 yyyyMMdd 형식이어야 합니다.");
        }
        try {
            return LocalDate.parse(dateText, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("조회 날짜가 올바르지 않습니다.");
        }
    }

    private record InquiryPeriod(LocalDate startDate, LocalDate endDate) {
        private String startDateText() {
            return startDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        }

        private String endDateText() {
            return endDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        }
    }
}
