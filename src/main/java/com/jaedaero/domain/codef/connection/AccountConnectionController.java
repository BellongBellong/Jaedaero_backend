package com.jaedaero.domain.codef.connection;

import com.jaedaero.domain.codef.account.CodefAccountSyncService;
import com.jaedaero.domain.codef.account.ConnectedAccountResponse;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 클라이언트 애플리케이션에서 사용하는 공개 계좌 연동 API입니다. */
@Api(tags = "계좌 연동")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountConnectionController {

  private final CodefConnectionService connectionService;
  private final CodefPersistenceRepository repository;
  private final CodefAccountSyncService accountSyncService;

  public AccountConnectionController(
      CodefConnectionService connectionService,
      CodefPersistenceRepository repository,
      CodefAccountSyncService accountSyncService) {
    this.connectionService = connectionService;
    this.repository = repository;
    this.accountSyncService = accountSyncService;
  }

  @ApiOperation(
      value = "계좌 연동 시작",
      notes =
          "CODEF에 금융기관을 등록하고 계좌를 동기화합니다. 로그인 정보는 CODEF 요청에만 사용하며, "
              + "Connected ID와 실제 계좌번호는 응답에 포함하지 않습니다. JWT 도입 전까지 userId를 요청 본문으로 받습니다.")
  @PostMapping("/connect")
  public ResponseEntity<CodefConnectionResponse> connect(
      @Valid @RequestBody CodefBankConnectionCreateRequest request) {
    CodefConnectionResponse response = connectionService.connect(request.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @ApiOperation(
      value = "연동 계좌 목록 조회",
      notes =
          "저장된 연동 계좌를 조회합니다. refresh=true일 때만 CODEF에서 최신 계좌 정보를 다시 가져옵니다. "
              + "실제 계좌번호와 암호화 값은 응답에 포함하지 않습니다.")
  @GetMapping
  public List<ConnectedAccountResponse> getAccounts(
      @ApiParam(value = "사용자 ID. JWT 도입 후 인증 사용자 ID로 대체합니다.", required = true, example = "1")
          @RequestParam
          long userId,
      @ApiParam(value = "CODEF에서 최신 계좌 정보를 다시 조회할지 여부", example = "false")
          @RequestParam(defaultValue = "false")
          boolean refresh) {
    if (refresh) {
      accountSyncService.refreshAllAccounts(userId);
    }
    return repository.findAccountsByUserId(userId).stream()
        .map(ConnectedAccountResponse::new)
        .toList();
  }
}
