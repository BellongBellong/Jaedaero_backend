package com.jaedaero.domain.codef.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaedaero.domain.codef.client.CodefApiClient;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredCodefConnection;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import com.jaedaero.global.security.SensitiveValueCipher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Queries CODEF securities assets and stock holdings without persisting sensitive portfolio
 * details.
 */
@Service
public class CodefSecuritiesInquiryService {
  private static final String FINANCIAL_ASSETS_PATH = "/v1/kr/stock/a/account/financial-assets";
  private static final String BALANCE_INQUIRY_PATH = "/v1/kr/stock/a/account/balance-inquiry";

  private final CodefApiClient codefApiClient;
  private final CodefPersistenceRepository repository;
  private final SensitiveValueCipher cipher;

  public CodefSecuritiesInquiryService(
      CodefApiClient codefApiClient,
      CodefPersistenceRepository repository,
      SensitiveValueCipher cipher) {
    this.codefApiClient = codefApiClient;
    this.repository = repository;
    this.cipher = cipher;
  }

  public SecuritiesAssetResponse getFinancialAssets(long userId, long accountId) {
    return inquire(userId, accountId, FINANCIAL_ASSETS_PATH, false);
  }

  public SecuritiesAssetResponse getStockHoldings(long userId, long accountId) {
    return inquire(userId, accountId, BALANCE_INQUIRY_PATH, true);
  }

  private SecuritiesAssetResponse inquire(
      long userId, long accountId, String path, boolean stockOnly) {
    StoredConnectedAccount account =
        repository
            .findAccountByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자의 연동 계좌를 찾을 수 없습니다."));
    if (!"ST".equals(account.businessType())) {
      throw new IllegalArgumentException("증권 계좌에 대해서만 조회할 수 있습니다.");
    }
    StoredCodefConnection connection =
        repository
            .findConnectionByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("연결된 CODEF 계정이 없습니다."));
    Map<String, String> body = new LinkedHashMap<>();
    body.put("connectedId", cipher.decrypt(connection.connectedIdEncrypted()));
    body.put("organization", account.institutionCode());
    body.put("account", cipher.decrypt(account.accountNumberEncrypted()));
    body.put("inquiryType", "0");

    JsonNode data = codefApiClient.postProduct(path, body).path("data");
    List<SecuritiesHoldingResponse> holdings = toHoldings(data, stockOnly);
    return new SecuritiesAssetResponse(
        account.accountId(),
        account.accountMasked(),
        number(data.path("resDepositReceived")),
        holdings);
  }

  private List<SecuritiesHoldingResponse> toHoldings(JsonNode data, boolean stockOnly) {
    JsonNode rows =
        data.isArray() ? data : firstArray(data, "resItemList", "resStockList", "resBalanceList");
    List<SecuritiesHoldingResponse> holdings = new ArrayList<>();
    if (!rows.isArray()) {
      return holdings;
    }
    for (JsonNode row : rows) {
      String typeCode = text(row, "resProductTypeCd", "resItemTypeCode");
      String type = text(row, "resProductType", "resItemType", "resBalanceType");
      if (stockOnly && !("01".equals(typeCode) || type.contains("주식"))) {
        continue;
      }
      holdings.add(
          new SecuritiesHoldingResponse(
              type,
              typeCode,
              text(row, "resItemName", "resStockName"),
              text(row, "resItemCode", "resStockCode"),
              text(row, "resQuantity", "resStockQuantity"),
              number(first(row, "resPurchaseAmount", "resBuyAmount")),
              number(first(row, "resValuationAmt", "resEvaluationAmount")),
              number(first(row, "resValuationPL", "resEvaluationProfitLoss")),
              text(row, "resEarningsRate", "resProfitRate"),
              number(first(row, "resPresentAmt", "resCurrentPrice")),
              text(row, "resAccountCurrency", "resCurrency")));
    }
    return holdings;
  }

  private JsonNode firstArray(JsonNode node, String... fields) {
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (value.isArray()) return value;
    }
    return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
  }

  private JsonNode first(JsonNode node, String... fields) {
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (!value.isMissingNode() && !value.isNull()) return value;
    }
    return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
  }

  private String text(JsonNode node, String... fields) {
    return first(node, fields).asText("").trim();
  }

  private long number(JsonNode node) {
    String value = node.asText("").replaceAll("[^0-9-]", "");
    if (value.isBlank() || "-".equals(value)) return 0L;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return 0L;
    }
  }
}
