package com.jaedaero.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.codef.common.CodefApiClient;
import com.jaedaero.codef.connection.CodefRsaEncryptor;
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
 * Development-only controller for KB Kookmin Bank demand-deposit transaction history.
 *
 * <p>It returns raw CODEF data, including transaction descriptions and account information. Do not
 * enable it in production.
 */
@Api(tags = "CODEF - 개발 검증")
@RestController
@RequestMapping("/api/codef/accounts")
public class CodefKookminTransactionListTestController {

    private static final String KOOKMIN_BANK_ORGANIZATION = "0004";
    private static final String TRANSACTION_LIST_PATH = "/v1/kr/bank/p/account/transaction-list";

    private final CodefApiClient codefApiClient;
    private final String publicKey;
    private final boolean enabled;
    private final CodefRsaEncryptor rsaEncryptor = new CodefRsaEncryptor();

    public CodefKookminTransactionListTestController(
            CodefApiClient codefApiClient,
            @Value("${codef.public-key:}") String publicKey,
            @Value("${codef.debug.account-create-enabled:false}") boolean enabled) {
        this.codefApiClient = codefApiClient;
        this.publicKey = publicKey;
        this.enabled = enabled;
    }

    @ApiOperation(
            value = "KB국민은행 입출금 거래내역 조회 테스트",
            notes = "개발 로컬 전용입니다. 기관코드 0004, orderBy=0, inquiryType=1로 요청합니다. "
                    + "거래 적요·계좌번호 등 민감한 원본 응답을 반환하므로 운영 환경에서 사용하면 안 됩니다.")
    @PostMapping("/test/kookmin/transactions")
    public JsonNode getKookminTransactionList(
            @Valid @RequestBody CodefKookminTransactionListTestRequest request) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("connectedId", request.getConnectedId());
        body.put("organization", KOOKMIN_BANK_ORGANIZATION);
        body.put("account", request.getAccount());
        body.put("startDate", request.getStartDate());
        body.put("endDate", request.getEndDate());
        body.put("orderBy", "0");
        body.put("inquiryType", "1");

        if (request.getAccountPassword() != null && !request.getAccountPassword().isBlank()) {
            body.put("accountPassword", rsaEncryptor.encrypt(request.getAccountPassword(), publicKey));
        }

        return codefApiClient.post(TRANSACTION_LIST_PATH, body);
    }
}
