package com.jaedaero.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;

/** Development-only request for querying resources through an existing CODEF Connected ID. */
@ApiModel(description = "CODEF Connected ID 기반 보유계좌 조회 요청")
public class CodefConnectedIdRequest {

    @NotBlank
    @ApiModelProperty(value = "CODEF 계정 생성 성공 시 발급된 Connected ID", required = true)
    private String connectedId;

    public String getConnectedId() {
        return connectedId;
    }

    public void setConnectedId(String connectedId) {
        this.connectedId = connectedId;
    }
}
