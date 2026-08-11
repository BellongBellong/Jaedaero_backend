package com.jaedaero.domain.codef.connection;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.codef.exception.CodefUserNotFoundException;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import org.junit.jupiter.api.Test;

class CodefConnectionServiceTest {

  @Test
  void rejectsUnknownLocalUserBeforeCallingCodef() {
    CodefConnectionService service =
        new CodefConnectionService(null, new MissingUserRepository(), null, null, null);
    CodefBankConnectionCreateRequest request = new CodefBankConnectionCreateRequest();
    request.setOrganizationCode("0020");
    request.setBusinessType("BK");
    request.setLoginId("test-user");
    request.setPassword("test-password");

    assertThrows(CodefUserNotFoundException.class, () -> service.connect(999L, request));
  }

  private static class MissingUserRepository extends CodefPersistenceRepository {
    private MissingUserRepository() {
      super(null);
    }

    @Override
    public boolean existsUser(long userId) {
      return false;
    }
  }
}
