package com.jaedaero.codef.connection;

import com.jaedaero.codef.account.CodefAccountSyncService;
import com.jaedaero.codef.institution.CodefBankInstitution;
import com.jaedaero.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.codef.persistence.StoredCodefConnection;
import com.jaedaero.codef.security.SensitiveValueCipher;
import com.jaedaero.codef.security.Sha256Hasher;
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
        CodefBankInstitution.fromOrganizationCode(request.getOrganizationCode());
        CodefAccountCreateResponse codefResponse = repository.findConnectionByUserId(userId)
                .map(connection -> accountClient.addAccount(cipher.decrypt(connection.connectedIdEncrypted()), request.toCodefRequest()))
                .orElseGet(() -> accountClient.createAccount(request.toCodefRequest()));

        if (codefResponse.getConnectedId() == null || codefResponse.getConnectedId().isBlank()) {
            throw new IllegalStateException("CODEF Connected ID 발급에 실패했습니다.");
        }
        repository.saveConnection(userId, cipher.encrypt(codefResponse.getConnectedId()), hasher.hash(codefResponse.getConnectedId()));
        int count = syncService.syncBankAccounts(userId, request.getOrganizationCode());
        return new CodefConnectionResponse(userId, request.getOrganizationCode(), count,
                codefResponse.getSuccessList(), codefResponse.getErrorList());
    }
}
