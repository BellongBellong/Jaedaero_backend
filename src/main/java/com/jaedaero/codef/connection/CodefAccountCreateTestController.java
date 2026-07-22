package com.jaedaero.codef.connection;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;

/**
 * Development-only endpoint for validating a real CODEF Connected ID issuance.
 *
 * <p>This endpoint must never be enabled in production. It intentionally returns the Connected ID
 * once so that developers can confirm CODEF account creation before database persistence is added.
 */
@Api(tags = "CODEF - 개발 검증")
@RestController
@RequestMapping("/api/codef/account-registrations")
public class CodefAccountCreateTestController {

    private final CodefAccountClient codefAccountClient;
    private final boolean enabled;

    public CodefAccountCreateTestController(
            CodefAccountClient codefAccountClient,
            @Value("${codef.debug.account-create-enabled:false}") boolean enabled) {
        this.codefAccountClient = codefAccountClient;
        this.enabled = enabled;
    }

    @ApiOperation(
            value = "CODEF Connected ID 발급 테스트",
            notes = "개발 로컬 전용입니다. 실제 은행 인증정보를 사용하며, Connected ID는 아직 DB에 저장되지 않습니다.")
    @PostMapping("/test")
    public CodefAccountCreateResponse createAccountForTest(
            @RequestBody CodefAccountCreateRequest request) {
        validateEnabled();
        return codefAccountClient.createAccount(request);
    }

    @ApiOperation(
            value = "KB국민은행 ID/PW Connected ID 발급 테스트",
            notes = "KB국민은행 개인 인터넷뱅킹(ID/PW) 전용 개발 테스트입니다. 기관코드 0004와 로그인 타입 1을 서버에서 고정합니다. "
                    + "은행 비밀번호는 저장하거나 로그에 남기지 않으며, 실제 Connected ID는 응답 확인용으로만 반환합니다.")
    @PostMapping("/test/kookmin")
    public CodefAccountCreateResponse createKookminAccountForTest(
            @Valid @RequestBody KookminBankAccountCreateTestRequest request) {
        validateEnabled();
        return codefAccountClient.createAccount(request.toCodefRequest());
    }

    private void validateEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
