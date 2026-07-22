package com.jaedaero.codef.institution;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Supplies the bank-button catalogue to the frontend. */
@Api(tags = "CODEF - 은행 기관")
@RestController
@RequestMapping("/api/codef/institutions")
public class CodefBankInstitutionController {

    @ApiOperation(value = "연결 가능한 은행 목록", notes = "프론트엔드는 organizationCode를 계정 연결 요청에 그대로 전달합니다.")
    @GetMapping("/banks")
    public List<CodefBankInstitutionResponse> getBanks() {
        return Arrays.stream(CodefBankInstitution.values())
                .map(CodefBankInstitutionResponse::new)
                .collect(Collectors.toList());
    }
}
