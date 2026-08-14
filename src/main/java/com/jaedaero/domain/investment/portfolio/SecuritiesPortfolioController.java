package com.jaedaero.domain.investment.portfolio;

import com.jaedaero.domain.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.domain.codef.account.SecuritiesAssetResponse;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import springfox.documentation.annotations.ApiIgnore;

/** 프런트가 CODEF 세부 호출을 알 필요 없이 증권 계좌 화면을 구성하도록 돕는 API입니다. */
@Api(tags = "투자 현황")
@RestController
@RequestMapping("/api/v1/investments/securities")
public class SecuritiesPortfolioController {

  private final CodefPersistenceRepository repository;
  private final CodefSecuritiesInquiryService securitiesInquiryService;

  public SecuritiesPortfolioController(
      CodefPersistenceRepository repository, CodefSecuritiesInquiryService securitiesInquiryService) {
    this.repository = repository;
    this.securitiesInquiryService = securitiesInquiryService;
  }

  @GetMapping
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "로컬 개발 환경에서 사용할 사용자 ID. 운영에서는 Bearer JWT를 사용합니다.",
      required = false,
      paramType = "header",
      example = "1")
  @ApiOperation(
      value = "내 증권 계좌 포트폴리오 조회",
      notes = "연결된 모든 증권 계좌의 계좌정보, 실시간 예수금, 전체 보유상품, 주식 보유종목을 한 번에 반환합니다.")
  public List<SecuritiesPortfolioResponse> getPortfolio(@ApiIgnore Authentication authentication) {
    long userId = authenticatedUserId(authentication);
    return repository.findAccountsByUserId(userId).stream()
        .filter(account -> "ST".equals(account.businessType()))
        .map(account -> toResponse(userId, account))
        .toList();
  }

  private SecuritiesPortfolioResponse toResponse(long userId, StoredConnectedAccount account) {
    SecuritiesAssetResponse assets =
        securitiesInquiryService.getFinancialAssets(userId, account.accountId());
    SecuritiesAssetResponse stocks = securitiesInquiryService.getStockHoldings(userId, account.accountId());
    return new SecuritiesPortfolioResponse(
        account.accountId(),
        account.institutionName(),
        account.accountMasked(),
        account.productName(),
        account.currentBalance(),
        account.availableBalance(),
        assets.depositAmount(),
        assets.holdings(),
        stocks.holdings());
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (RuntimeException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자 인증 정보입니다.");
    }
  }
}
