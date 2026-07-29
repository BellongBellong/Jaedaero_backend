package com.jaedaero.domain.codef.connection;

import com.jaedaero.domain.codef.account.CodefAccountSyncService;
import com.jaedaero.domain.codef.exception.CodefApiException;
import com.jaedaero.domain.codef.exception.CodefUserNotFoundException;
import com.jaedaero.domain.codef.institution.CodefBusinessType;
import com.jaedaero.domain.codef.institution.CodefBankInstitution;
import com.jaedaero.domain.codef.institution.CodefSecuritiesInstitution;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredCodefConnection;
import com.jaedaero.global.security.SensitiveValueCipher;
import com.jaedaero.global.security.Sha256Hasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodefConnectionService {
    private final CodefAccountClient accountClient;
    private final CodefPersistenceRepository repository;
    private final SensitiveValueCipher cipher;
    private final Sha256Hasher hasher;
    private final CodefAccountSyncService syncService;

    public CodefConnectionService(CodefAccountClient accountClient, CodefPersistenceRepository repository,
            SensitiveValueCipher cipher, Sha256Hasher hasher, CodefAccountSyncService syncService) {
        this.accountClient = accountClient;
        this.repository = repository;
        this.cipher = cipher;
        this.hasher = hasher;
        this.syncService = syncService;
    }

    @Transactional
    public CodefConnectionResponse connect(long userId, CodefBankConnectionCreateRequest request) {
        if (!repository.existsUser(userId)) {
            throw new CodefUserNotFoundException(userId);
        }
        CodefBusinessType businessType = CodefBusinessType.fromCode(request.getBusinessType());
        String businessTypeCode = businessType.getCode();
        String organizationCode = request.getOrganizationCode();
        validateInstitution(organizationCode, businessType);
        if (repository.findInstitutionConnection(userId, organizationCode, businessTypeCode)
                .filter(connection -> "ACTIVE".equals(connection.status()))
                .map(connection -> {
                    repository.saveInstitutionConnection(
                            connection.connectionId(), organizationCode, businessTypeCode,
                            CodefAccountCreateRequest.ID_PASSWORD_LOGIN_TYPE,
                            cipher.encrypt(request.getLoginId()), cipher.encrypt(request.getPassword()),
                            encryptIfPresent(request.getBirthDate()));
                    return connection;
                })
                .isPresent()) {
            int count = syncService.syncAccounts(userId, organizationCode, businessType);
            return CodefConnectionResponse.alreadyConnected(userId, organizationCode, count);
        }

        StoredCodefConnection existingConnection = repository.findConnectionByUserId(userId).orElse(null);
        CodefAccountCreateResponse codefResponse = existingConnection == null
                ? accountClient.createAccount(request.toCodefRequest())
                : accountClient.addAccount(
                        cipher.decrypt(existingConnection.connectedIdEncrypted()), request.toCodefRequest());

        if (!codefResponse.isOrganizationRegistered(organizationCode)) {
            throw new CodefApiException(
                    "CODEF 기관 등록 실패 [" + organizationCode + "]: "
                            + codefResponse.organizationErrorMessage(organizationCode),
                    422);
        }
        String connectedId = codefResponse.getConnectedId();
        if ((connectedId == null || connectedId.isBlank()) && existingConnection != null) {
            connectedId = cipher.decrypt(existingConnection.connectedIdEncrypted());
        }
        if (connectedId == null || connectedId.isBlank()) {
            throw new IllegalStateException("CODEF Connected ID 발급에 실패했습니다.");
        }
        repository.saveConnection(userId, cipher.encrypt(connectedId), hasher.hash(connectedId));
        StoredCodefConnection connection = repository.findConnectionByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("CODEF 연결 저장에 실패했습니다."));
        repository.saveInstitutionConnection(
                connection.connectionId(), organizationCode, businessTypeCode,
                CodefAccountCreateRequest.ID_PASSWORD_LOGIN_TYPE,
                cipher.encrypt(request.getLoginId()), cipher.encrypt(request.getPassword()),
                encryptIfPresent(request.getBirthDate()));
        int count = syncService.syncAccounts(userId, organizationCode, businessType);
        return new CodefConnectionResponse(userId, organizationCode, count,
                codefResponse.getSuccessList(), codefResponse.getErrorList());
    }

    @Transactional
    public int syncRegisteredInstitution(long userId, String organizationCode, String businessTypeCode) {
        CodefBusinessType businessType = CodefBusinessType.fromCode(businessTypeCode);
        validateInstitution(organizationCode, businessType);
        repository.findInstitutionConnection(userId, organizationCode, businessType.getCode())
                .filter(connection -> "ACTIVE".equals(connection.status()))
                .orElseThrow(() -> new IllegalStateException("먼저 해당 금융기관을 CODEF에 연결해야 합니다."));
        return syncService.syncAccounts(userId, organizationCode, businessType);
    }

    private void validateInstitution(String organizationCode, CodefBusinessType businessType) {
        if (businessType == CodefBusinessType.SECURITIES) {
            CodefSecuritiesInstitution.fromOrganizationCode(organizationCode);
            return;
        }
        CodefBankInstitution.fromOrganizationCode(organizationCode);
    }

    private String encryptIfPresent(String value) {
        return value == null || value.isBlank() ? null : cipher.encrypt(value);
    }
}
