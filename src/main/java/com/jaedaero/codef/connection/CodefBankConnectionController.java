package com.jaedaero.codef.connection;

import com.jaedaero.codef.institution.CodefBankInstitution;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generic bank connection API.
 *
 * <p>When member authentication and persistence are added, connectedId must be loaded and encrypted
 * by the server rather than accepted from the client.
 */
@Api(tags = "CODEF - 은행 연결")
@RestController
@RequestMapping("/api/codef/connections")
public class CodefBankConnectionController {

    private final CodefAccountClient codefAccountClient;
    private final boolean enabled;

    public CodefBankConnectionController(
            CodefAccountClient codefAccountClient,
            @Value("${codef.debug.account-create-enabled:false}") boolean enabled) {
        this.codefAccountClient = codefAccountClient;
        this.enabled = enabled;
    }

    @ApiOperation(
            value = "선택한 은행 연결",
            notes = "connectedId가 없으면 최초 Connected ID를 발급하고, 있으면 해당 Connected ID에 은행을 추가합니다. "
                    + "현재는 회원 DB 연동 전 단계이므로 Connected ID를 응답으로 반환합니다.")
    @PostMapping
    public CodefAccountCreateResponse connectBank(
            @Valid @RequestBody CodefBankConnectionCreateRequest request) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        CodefBankInstitution.fromOrganizationCode(request.getOrganizationCode());
        if (request.getConnectedId() == null || request.getConnectedId().isBlank()) {
            return codefAccountClient.createAccount(request.toCodefRequest());
        }
        return codefAccountClient.addAccount(request.getConnectedId(), request.toCodefRequest());
    }
}
