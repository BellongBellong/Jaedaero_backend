package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import springfox.documentation.annotations.ApiIgnore;

/** 로컬 동기화 거래 캐시를 사용하는 거래내역 조회 및 수정 API입니다. */
@RestController
@RequestMapping("/api/v1/transactions")
@Api(tags = "거래내역")
public class TransactionController {

  private final CodefPersistenceRepository repository;
  private final CodefTransactionSyncService transactionSyncService;

  @Autowired
  public TransactionController(
      CodefPersistenceRepository repository, CodefTransactionSyncService transactionSyncService) {
    this.repository = repository;
    this.transactionSyncService = transactionSyncService;
  }

  /** Test-only convenience constructor. Production requests always use the sync-enabled constructor. */
  public TransactionController(CodefPersistenceRepository repository) {
    this(repository, null);
  }

  @GetMapping
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 사용자 ID. 운영에서는 JWT 인증으로 대체됩니다.",
      required = true,
      paramType = "header",
      example = "1")
  @ApiOperation(
      value = "거래내역 조회",
      notes = "동기화된 거래내역을 조회합니다. accountId, 기간, 카테고리는 선택 필터이며 CODEF를 다시 호출하지 않습니다.")
  public List<TransactionResponse> getTransactions(
      @ApiIgnore Authentication authentication,
      @ApiParam(value = "연결 계좌 ID. 생략하면 사용자의 모든 연동 계좌를 조회합니다.", example = "10")
          @RequestParam(required = false)
          Long accountId,
      @ApiParam(value = "조회 시작일(yyyy-MM-dd). 생략하면 종료일 기준 3개월 전입니다.", example = "2026-04-30")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @ApiParam(value = "조회 종료일(yyyy-MM-dd). 생략하면 오늘입니다.", example = "2026-07-30")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @ApiParam(value = "거래 카테고리 코드 완전 일치 필터", example = "FOOD")
          @RequestParam(required = false)
          String category) {
    long userId = authenticatedUserId(authentication);
    LocalDate resolvedEndDate = endDate == null ? LocalDate.now() : endDate;
    LocalDate resolvedStartDate = startDate == null ? resolvedEndDate.minusMonths(3) : startDate;
    if (resolvedStartDate.isAfter(resolvedEndDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate는 endDate보다 늦을 수 없습니다.");
    }
    synchronizeMissingPeriods(userId, accountId, resolvedStartDate, resolvedEndDate);
    return repository
        .findTransactionsByUser(userId, accountId, resolvedStartDate, resolvedEndDate, category)
        .stream()
        .map(TransactionResponse::new)
        .collect(Collectors.toList());
  }

  private void synchronizeMissingPeriods(
      long userId, Long accountId, LocalDate startDate, LocalDate endDate) {
    if (transactionSyncService == null) {
      return;
    }
    List<StoredConnectedAccount> accounts =
        accountId == null
            ? repository.findAccountsByUserId(userId)
            : repository.findAccountByIdAndUserId(accountId, userId).stream().toList();
    for (StoredConnectedAccount account : accounts) {
      if (!"DEMAND_DEPOSIT".equals(account.accountType())
          || repository.isTransactionPeriodCovered(
              account.accountId(), "DEMAND_DEPOSIT", startDate, endDate)) {
        continue;
      }
      transactionSyncService.sync(
          userId,
          account.accountId(),
          startDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
          endDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
      repository.recordTransactionSyncPeriod(
          account.accountId(), "DEMAND_DEPOSIT", startDate, endDate);
    }
  }

  @PutMapping("/{transactionId}/category")
  @ApiOperation(
      value = "거래내역 카테고리 수정",
      notes = "본인 소유의 거래내역만 수정할 수 있습니다. 카테고리는 9개 고정값 중 하나여야 합니다.")
  public TransactionCategoryUpdateResponse updateTransactionCategory(
      @ApiIgnore Authentication authentication,
      @ApiParam(value = "거래내역 ID", required = true, example = "100") @PathVariable long transactionId,
      @Valid @RequestBody TransactionCategoryUpdateRequest request) {
    long userId = authenticatedUserId(authentication);
    int updated =
        repository.updateTransactionCategoryByUser(
            transactionId, userId, request.getCategory().name());
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "거래내역을 찾을 수 없습니다.");
    }
    return new TransactionCategoryUpdateResponse(transactionId, request.getCategory());
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자 인증 정보입니다.");
    }
  }
}
