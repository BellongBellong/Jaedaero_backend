package com.jaedaero.domain.investment.recommendation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "위험등급별 ETF 상품 목록")
public record RiskLevelRecommendationGroup(
    @ApiModelProperty(value = "위험등급") RiskLevel riskLevel,
    @ApiModelProperty(value = "리밸런싱 자산 바구니") AssetBucket assetBucket,
    @ApiModelProperty(value = "초기 추천 대상 포함 여부") boolean eligibleForRecommendation,
    @ApiModelProperty(value = "분류된 ETF 수") int count,
    @ApiModelProperty(value = "ETF 목록") List<EtfProductRecommendation> items) {}
