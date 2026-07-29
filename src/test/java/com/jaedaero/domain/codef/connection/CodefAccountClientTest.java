package com.jaedaero.domain.codef.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.codef.client.CodefApiClient;
import com.jaedaero.domain.codef.institution.CodefBusinessType;
import com.jaedaero.domain.codef.token.CodefAccessTokenProvider;
import com.jaedaero.domain.codef.token.CodefTokenClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CodefAccountClientTest {

    private static final String PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApGjZkdV5NrbUDhIRoLbyb00WGg42hC7OYXZeEvD86xGnRtTjOUn8IofRUam99MeIv2U0Gz4ycSvMJul9VXbh9m4J1XyxSXU0H5szQF/XHAvaB784QgG2qb2amVcqPTUtmCyPfHAn4QDm3rceF18NjzsR0mITTdtgYlQW/3piVIVjH7KoTOh6W9jXGfeQaQIhqfXBf6a5duo3KH8rxqSpwQ/xGAQwEzGS14Fl4WqgGhTDyL7XQ4nK2pcTpoEMxRGB+x7h5MM+ZCiEoU7uMPy6sVRjQZcs/KnKvfGsve/L0QvNHIbjWS3SGchnXXbgE6bWozFsQd64pT71pI4C0QvpWQIDAQAB";

    @Test
    void createAccount_encryptsPasswordAndBuildsCodefAccountCreateRequest() throws Exception {
        RecordingCodefApiClient apiClient = new RecordingCodefApiClient();
        CodefAccountClient accountClient = new CodefAccountClient(
                apiClient, PUBLIC_KEY, new CodefRsaEncryptor());

        CodefAccountCreateResponse response = accountClient.createAccount(idPasswordRequest());

        assertEquals("/v1/account/create", apiClient.path);
        Map<String, Object> requestBody = apiClient.body;
        Map<String, Object> account = firstAccount(requestBody);
        assertEquals("0004", account.get("organization"));
        assertEquals("1", account.get("loginType"));
        assertEquals("bank-user", account.get("id"));
        assertNotEquals("bank-password", account.get("password"));
        assertEquals("connected-id", response.getConnectedId());
        assertEquals("CF-00000", response.getSuccessList().get(0).getCode());
    }

    @Test
    void addAccount_sendsExistingConnectedId() throws Exception {
        RecordingCodefApiClient apiClient = new RecordingCodefApiClient();
        CodefAccountClient accountClient = new CodefAccountClient(
                apiClient, PUBLIC_KEY, new CodefRsaEncryptor());

        accountClient.addAccount("existing-connected-id", idPasswordRequest());

        assertEquals("/v1/account/add", apiClient.path);
        assertEquals("existing-connected-id", apiClient.body.get("connectedId"));
    }

    @Test
    void securitiesConnection_usesIntegratedClientTypeRequiredByCodef() {
        CodefBankConnectionCreateRequest request = new CodefBankConnectionCreateRequest();
        request.setBusinessType(CodefBusinessType.SECURITIES.getCode());
        request.setOrganizationCode("0238");
        request.setLoginId("mirae-user");
        request.setPassword("mirae-password");

        CodefAccountCreateRequest codefRequest = request.toCodefRequest();

        assertEquals("ST", codefRequest.getBusinessType());
        assertEquals("A", codefRequest.getClientType());
    }

    private CodefAccountCreateRequest idPasswordRequest() {
        CodefAccountCreateRequest request = new CodefAccountCreateRequest();
        request.setOrganization("0004");
        request.setLoginId("bank-user");
        request.setPassword("bank-password");
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstAccount(Map<String, Object> requestBody) {
        return (Map<String, Object>) ((java.util.List<?>) requestBody.get("accountList")).get(0);
    }

    private static class RecordingCodefApiClient extends CodefApiClient {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private String path;
        private Map<String, Object> body;

        RecordingCodefApiClient() {
            super(
                    "http://localhost",
                    new CodefAccessTokenProvider(new CodefTokenClient(
                            "http://localhost/token", "unused", "unused", 1)));
        }

        @Override
        @SuppressWarnings("unchecked")
        public JsonNode post(String path, Object requestBody) {
            this.path = path;
            this.body = (Map<String, Object>) requestBody;
            try {
                return objectMapper.readTree("""
                        {
                          "result": {"code": "CF-00000"},
                          "data": {
                            "connectedId": "connected-id",
                            "successList": [{"organization": "0004", "code": "CF-00000", "message": "성공"}],
                            "errorList": []
                          }
                        }
                        """);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }
    }
}
