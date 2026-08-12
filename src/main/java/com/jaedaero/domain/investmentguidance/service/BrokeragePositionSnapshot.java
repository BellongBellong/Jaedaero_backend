package com.jaedaero.domain.investmentguidance.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BrokeragePositionSnapshot(
    long accountValue,
    long safeAssetAmount,
    long riskAssetAmount,
    long investmentPrincipal,
    long marketValue,
    long unrealizedProfitLoss,
    BigDecimal returnRate,
    LocalDateTime marketDataAsOf) {}
