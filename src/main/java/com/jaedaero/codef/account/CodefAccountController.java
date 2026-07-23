package com.jaedaero.codef.account;

import com.jaedaero.codef.persistence.CodefPersistenceRepository;
import io.swagger.annotations.ApiOperation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/codef")
public class CodefAccountController {
    private final CodefPersistenceRepository repository;
    private final CodefAccountSyncService syncService;
    private final CodefTransactionSyncService transactionSyncService;
    private final CodefSavingsTransactionSyncService savingsTransactionSyncService;

    public CodefAccountController(
            CodefPersistenceRepository repository,
            CodefAccountSyncService syncService,
            CodefTransactionSyncService transactionSyncService,
            CodefSavingsTransactionSyncService savingsTransactionSyncService) {
        this.repository = repository;
        this.syncService = syncService;
        this.transactionSyncService = transactionSyncService;
        this.savingsTransactionSyncService = savingsTransactionSyncService;
    }

    @GetMapping("/accounts")
    public List<ConnectedAccountResponse> getAccounts(
            @RequestParam long userId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        if (refresh) syncService.refreshAllAccounts(userId);
        return repository.findAccountsByUserId(userId).stream()
                .map(ConnectedAccountResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/savings")
    public List<ConnectedAccountResponse> getSavings(
            @RequestParam long userId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        if (refresh) syncService.refreshAllAccounts(userId);
        return repository.findSavingsByUserId(userId).stream()
                .map(ConnectedAccountResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "적금 거래내역 조회",
            notes = "기본 최근 3개월을 저장하며, 요청 기간이 DB 동기화 범위를 벗어난 경우에만 CODEF를 다시 호출합니다.")
    @GetMapping("/savings/{accountId}/transactions")
    public List<TransactionResponse> getSavingsTransactions(
            @org.springframework.web.bind.annotation.PathVariable long accountId,
            @RequestParam long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
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

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> getTransactions(
            @org.springframework.web.bind.annotation.PathVariable long accountId,
            @RequestParam long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
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
