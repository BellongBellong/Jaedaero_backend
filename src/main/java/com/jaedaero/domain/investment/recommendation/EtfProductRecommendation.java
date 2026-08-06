package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.domain.investment.etf.EtfDailyTradingInfo;
import com.jaedaero.domain.investment.etf.EtfReturnPeriod;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "KRX ETF 기반 상품 분류 결과")
public record EtfProductRecommendation(
    @ApiModelProperty(value = "ETF 종목코드", example = "069500") String isuCd,
    @ApiModelProperty(value = "ETF 종목명", example = "KODEX 200") String name,
    @ApiModelProperty(value = "내부 위험등급", example = "HIGH") RiskLevel riskLevel,
    @ApiModelProperty(value = "리밸런싱 자산 바구니", example = "RISK") AssetBucket assetBucket,
    @ApiModelProperty(value = "초기 추천 대상 포함 여부", example = "true") boolean eligibleForRecommendation,
    @ApiModelProperty(value = "분류 근거") List<String> classificationReasons,
    @ApiModelProperty(value = "KRX 일별매매정보") EtfDailyTradingInfo market,
    @ApiModelProperty(value = "기간별 수익률") List<EtfReturnPeriod> returns) {}
