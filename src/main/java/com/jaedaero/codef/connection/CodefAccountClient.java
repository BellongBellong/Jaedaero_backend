package com.jaedaero.codef.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.codef.common.CodefApiClient;
import com.jaedaero.codef.common.CodefApiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** CODEF Connected ID account-create client. */
@Component
public class CodefAccountClient {

    private final CodefApiClient codefApiClient;
    private final String publicKey;
    private final CodefRsaEncryptor rsaEncryptor;

    @Autowired
    public CodefAccountClient(
            CodefApiClient codefApiClient,
            @Value("${codef.public-key:}") String publicKey) {
        this(codefApiClient, publicKey, new CodefRsaEncryptor());
    }

    CodefAccountClient(
            CodefApiClient codefApiClient, String publicKey, CodefRsaEncryptor rsaEncryptor) {
        this.codefApiClient = codefApiClient;
        this.publicKey = publicKey;
        this.rsaEncryptor = rsaEncryptor;
    }

    public CodefAccountCreateResponse createAccount(CodefAccountCreateRequest accountCreateRequest) {
        validate(accountCreateRequest);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountList", List.of(toCodefAccount(accountCreateRequest)));

        return register("/v1/account/create", body);
    }

    /** Adds one bank institution to an already issued Connected ID. */
    public CodefAccountCreateResponse addAccount(
            String connectedId, CodefAccountCreateRequest accountCreateRequest) {
        if (isBlank(connectedId)) {
            throw new IllegalArgumentException("connectedId is required when adding an institution.");
        }
        validate(accountCreateRequest);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("connectedId", connectedId);
        body.put("accountList", List.of(toCodefAccount(accountCreateRequest)));

        return register("/v1/account/add", body);
    }

    private CodefAccountCreateResponse register(String path, Map<String, Object> body) {
        JsonNode response = codefApiClient.post(path, body);
        JsonNode result = response.path("result");
        if (!"CF-00000".equals(result.path("code").asText())) {
            String code = result.path("code").asText("UNKNOWN");
            String message = result.path("message").asText("CODEF 계정 등록 요청이 거절되었습니다.");
            String extraMessage = result.path("extraMessage").asText();
            String detail = extraMessage.isBlank() ? message : message + " (" + extraMessage + ")";
            throw new CodefApiException("CODEF 계정 등록 실패 [" + code + "]: " + detail, 422);
        }

        JsonNode data = response.path("data");
        return new CodefAccountCreateResponse(
                data.path("connectedId").asText(),
                registrationResults(data.path("successList")),
                registrationResults(data.path("errorList")));
    }

    private Map<String, Object> toCodefAccount(CodefAccountCreateRequest request) {
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("countryCode", request.getCountryCode());
        account.put("businessType", request.getBusinessType());
        account.put("clientType", request.getClientType());
        account.put("organization", request.getOrganization());
        account.put("loginType", request.getLoginType());
        account.put("password", rsaEncryptor.encrypt(request.getPassword(), publicKey));

        putIfPresent(account, "id", request.getLoginId());
        putIfPresent(account, "birthday", request.getBirthday());
        putIfPresent(account, "keyFile", request.getKeyFile());
        putIfPresent(account, "derFile", request.getDerFile());
        return account;
    }

    private void validate(CodefAccountCreateRequest request) {
        if (isBlank(request.getOrganization()) || isBlank(request.getLoginType()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("organization, loginType, and password are required.");
        }
        if ("1".equals(request.getLoginType()) && isBlank(request.getLoginId())) {
            throw new IllegalArgumentException("loginId is required for ID/PW login.");
        }
        if ("0".equals(request.getLoginType())
                && (isBlank(request.getKeyFile()) || isBlank(request.getDerFile()))) {
            throw new IllegalArgumentException("keyFile and derFile are required for certificate login.");
        }
    }

    private List<CodefAccountRegistrationResult> registrationResults(JsonNode results) {
        List<CodefAccountRegistrationResult> registrationResults = new ArrayList<>();
        if (!results.isArray()) {
            return registrationResults;
        }

        for (JsonNode result : results) {
            registrationResults.add(new CodefAccountRegistrationResult(
                    result.path("organization").asText(),
                    result.path("code").asText(),
                    result.path("message").asText()));
        }
        return registrationResults;
    }

    private void putIfPresent(Map<String, Object> account, String key, String value) {
        if (!isBlank(value)) {
            account.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
