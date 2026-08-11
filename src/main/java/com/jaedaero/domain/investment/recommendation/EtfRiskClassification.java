package com.jaedaero.domain.investment.recommendation;

import java.util.List;

public record EtfRiskClassification(
    RiskLevel riskLevel,
    AssetBucket assetBucket,
    boolean eligibleForRecommendation,
    List<String> classificationReasons) {}
