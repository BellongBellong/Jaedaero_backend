package com.jaedaero.domain.codef.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.domain.codef.account.SecuritiesAssetResponse;
import com.jaedaero.domain.codef.account.SecuritiesHoldingResponse;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CodefDemoServiceTest {

  @Test
  void getUnifiedAssets_groupsByInstitutionAndAddsBankAndSecuritiesAssets() {
    StubRepository repository =
        new StubRepository(
            List.of(
                account(1L, "0004", "BK", "DEMAND_DEPOSIT", "입출금", 100L),
                account(2L, "0238", "ST", "SECURITIES", "종합", 0L),
                account(3L, "0004", "BK", "대출", "신용대출", 9_999L)));
    StubSecuritiesInquiry inquiry = new StubSecuritiesInquiry(Map.of(2L, assets(50L, 300L, 150L)));
    CodefDemoService service = service(repository, inquiry);

    CodefDemoUnifiedAssets result = service.getUnifiedAssets(1L);

    assertEquals("600원", result.getTotalAmount());
    assertEquals(3, result.getRefreshedAccountCount());
    assertEquals(
        List.of("KB국민은행", "미래에셋증권"),
        result.getInstitutionGroups().stream()
            .map(CodefDemoInstitutionAssetGroup::getInstitutionName)
            .toList());

    CodefDemoUnifiedAccountAsset bank = result.getInstitutionGroups().get(0).getAccounts().get(0);
    assertEquals("100원", bank.getAssetAmount());
    assertTrue(bank.isBankDetailAvailable());
    assertFalse(bank.isSecurities());

    CodefDemoUnifiedAccountAsset securities =
        result.getInstitutionGroups().get(1).getAccounts().get(0);
    assertEquals("500원", securities.getAssetAmount());
    assertTrue(securities.isSecurities());
    assertEquals("증권 평가자산 기준", securities.getStatusMessage());
  }

  @Test
  void getUnifiedAssets_keepsCachedSecuritiesBalanceWhenIndividualInquiryFails() {
    StubRepository repository =
        new StubRepository(List.of(account(9L, "0238", "ST", "SECURITIES", "종합", 777L)));
    CodefDemoService service = service(repository, new StubSecuritiesInquiry(Map.of()));

    CodefDemoUnifiedAccountAsset account =
        service.getUnifiedAssets(1L).getInstitutionGroups().get(0).getAccounts().get(0);

    assertEquals("777원", account.getAssetAmount());
    assertEquals("증권 자산 조회 실패", account.getStatusMessage());
  }

  private CodefDemoService service(
      CodefPersistenceRepository repository,
      CodefSecuritiesInquiryService inquiry) {
    return new CodefDemoService(
        repository, null, null, null, inquiry, null, null);
  }

  private static StoredConnectedAccount account(
      long accountId,
      String institutionCode,
      String businessType,
      String accountType,
      String productName,
      long balance) {
    return new StoredConnectedAccount(
        accountId,
        1L,
        1L,
        institutionCode,
        businessType,
        institutionCode,
        "encrypted",
        "1234-****",
        accountType,
        productName,
        balance,
        null,
        null);
  }

  private static SecuritiesAssetResponse assets(long deposit, long... valuationAmounts) {
    List<SecuritiesHoldingResponse> holdings =
        java.util.Arrays.stream(valuationAmounts)
            .mapToObj(
                value ->
                    new SecuritiesHoldingResponse(
                        "주식", "01", "ETF", "069500", "1", 0L, value, 0L, "0", 0L, "KRW"))
            .toList();
    return new SecuritiesAssetResponse(2L, "1234-****", deposit, holdings);
  }

  private static class StubRepository extends CodefPersistenceRepository {
    private final List<StoredConnectedAccount> accounts;

    StubRepository(List<StoredConnectedAccount> accounts) {
      super(null);
      this.accounts = accounts;
    }

    @Override
    public List<StoredConnectedAccount> findAccountsByUserId(long userId) {
      return accounts;
    }
  }

  private static class StubSecuritiesInquiry extends CodefSecuritiesInquiryService {
    private final Map<Long, SecuritiesAssetResponse> responses;

    StubSecuritiesInquiry(Map<Long, SecuritiesAssetResponse> responses) {
      super(null, null, null);
      this.responses = responses;
    }

    @Override
    public SecuritiesAssetResponse getFinancialAssets(long userId, long accountId) {
      SecuritiesAssetResponse response = responses.get(accountId);
      if (response == null) {
        throw new IllegalStateException("증권 응답 없음");
      }
      return response;
    }
  }
}
