package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.domain.codef.account.SecuritiesAssetResponse;
import com.jaedaero.domain.codef.account.SecuritiesHoldingResponse;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CodefBrokeragePositionProvider implements BrokeragePositionProvider {

  private final CodefSecuritiesInquiryService securitiesInquiryService;
  private final Clock clock;

  public CodefBrokeragePositionProvider(
      CodefSecuritiesInquiryService securitiesInquiryService, Clock clock) {
    this.securitiesInquiryService = securitiesInquiryService;
    this.clock = clock;
  }

  @Override
  public BrokeragePositionSnapshot load(long userId, RecurringInvestmentPlanVo plan) {
    SecuritiesAssetResponse assets =
        securitiesInquiryService.getFinancialAssets(userId, plan.getBrokerageAccountId());
    List<SecuritiesHoldingResponse> selected =
        assets.holdings()
            .stream()
            .filter(holding -> matches(holding, plan))
            .toList();
    long principal = selected.stream().mapToLong(SecuritiesHoldingResponse::purchaseAmount).sum();
    long marketValue = selected.stream().mapToLong(SecuritiesHoldingResponse::valuationAmount).sum();
    long profitLoss = selected.stream().mapToLong(SecuritiesHoldingResponse::valuationProfit).sum();
    long riskAssetAmount =
        assets.holdings().stream().mapToLong(SecuritiesHoldingResponse::valuationAmount).sum();
    long accountValue =
        Math.addExact(assets.depositAmount(), riskAssetAmount);
    BigDecimal returnRate =
        principal == 0
            ? BigDecimal.ZERO.setScale(4)
            : BigDecimal.valueOf(profitLoss)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(principal), 4, RoundingMode.HALF_UP);
    return new BrokeragePositionSnapshot(
        accountValue,
        assets.depositAmount(),
        riskAssetAmount,
        principal,
        marketValue,
        profitLoss,
        returnRate,
        LocalDateTime.now(clock));
  }

  private boolean matches(
      SecuritiesHoldingResponse holding, RecurringInvestmentPlanVo plan) {
    String configuredCode = normalize(plan.getInvestmentProductCode());
    String actualCode = normalize(holding.itemCode());
    if (!configuredCode.isEmpty() && configuredCode.equalsIgnoreCase(actualCode)) {
      return true;
    }
    return normalize(plan.getInvestmentProductName())
        .equalsIgnoreCase(normalize(holding.itemName()));
  }

  private String normalize(String value) {
    return value == null ? "" : value.replace(" ", "").trim();
  }
}
