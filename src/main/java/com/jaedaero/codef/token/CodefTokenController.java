package com.jaedaero.codef.token;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Api(tags = "CODEF - 개발 검증")
@RestController
@RequestMapping("/api/codef/token")
public class CodefTokenController {

    private final CodefTokenClient codefTokenClient;
    private final String environment;

    public CodefTokenController(
            CodefTokenClient codefTokenClient,
            @Value("${app.environment:local}") String environment) {
        this.codefTokenClient = codefTokenClient;
        this.environment = environment;
    }

    @ApiOperation(
            value = "CODEF Access Token 발급 검증",
            notes = "local 또는 development 환경에서만 사용할 수 있습니다. Access Token 자체는 응답하지 않습니다.")
    @PostMapping("/verify")
    public CodefTokenVerificationResponse verifyTokenIssuance() {
        if (!isDevelopmentEnvironment()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        CodefTokenResponse tokenResponse = codefTokenClient.publishToken();
        return CodefTokenVerificationResponse.from(tokenResponse);
    }

    private boolean isDevelopmentEnvironment() {
        return "local".equalsIgnoreCase(environment)
                || "development".equalsIgnoreCase(environment);
    }
}
