package com.jaedaero.domain.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "증권 계좌의 종합자산 또는 주식 잔고 조회 결과")
public record SecuritiesAssetResponse(
        @ApiModelProperty(value = "조회한 계좌 ID", example = "10") long accountId,
        @ApiModelProperty(value = "마스킹된 계좌번호", example = "4555-****") String accountMasked,
        @ApiModelProperty(value = "예수금", example = "120000") long depositAmount,
        @ApiModelProperty(value = "상품 또는 종목별 보유 정보") List<SecuritiesHoldingResponse> holdings) {
    public SecuritiesAssetResponse {
        holdings = List.copyOf(holdings);
    }
}
