package com.jaedaero.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.codef.common.CodefApiClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Development-only controller for confirming the CODEF bank account-list response.
 *
 * <p>It returns raw CODEF data, including account information. Do not enable it in production.
 */
@Api(tags = "CODEF - 개발 검증")
@RestController
@RequestMapping("/api/codef/accounts")
public class CodefKookminAccountListTestController {

    private static final String KOOKMIN_BANK_ORGANIZATION = "0004";
    private static final String ACCOUNT_LIST_PATH = "/v1/kr/bank/p/account/account-list";

    private final CodefApiClient codefApiClient;
    private final boolean enabled;

    public CodefKookminAccountListTestController(
            CodefApiClient codefApiClient,
            @Value("${codef.debug.account-create-enabled:false}") boolean enabled) {
        this.codefApiClient = codefApiClient;
        this.enabled = enabled;
    }

    @ApiOperation(
            value = "KB국민은행 보유계좌 조회 테스트",
            notes = "개발 로컬 전용입니다. KB국민은행 기관코드 0004로 보유계좌를 조회합니다. "
                    + "잔액·계좌번호가 포함된 원본 응답을 반환하므로 운영 환경에서 사용하면 안 됩니다.")
    @PostMapping("/test/kookmin/list")
    public JsonNode getKookminAccountList(@Valid @RequestBody CodefConnectedIdRequest request) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("organization", KOOKMIN_BANK_ORGANIZATION);
        body.put("connectedId", request.getConnectedId());

        return codefApiClient.post(ACCOUNT_LIST_PATH, body);
    }
}
