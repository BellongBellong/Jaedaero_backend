package com.jaedaero.domain.codef.institution;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/** Supplies the bank-button catalogue to the frontend. */
@Api(tags = "CODEF 연결 가능 금융기관")
@RestController
@ApiIgnore
@RequestMapping("/api/v1/codef/institutions")
public class CodefBankInstitutionController {

  @ApiOperation(value = "연결 가능한 은행 목록", notes = "응답의 기관 코드를 은행 연결 요청의 기관 코드에 그대로 사용합니다.")
  @GetMapping("/banks")
  public List<CodefBankInstitutionResponse> getBanks() {
    return Arrays.stream(CodefBankInstitution.values())
        .map(CodefBankInstitutionResponse::new)
        .collect(Collectors.toList());
  }

  @ApiOperation(value = "연결 가능한 증권사 목록", notes = "응답의 기관 코드를 증권사 연결 요청의 기관 코드에 그대로 사용합니다.")
  @GetMapping("/securities")
  public List<CodefSecuritiesInstitutionResponse> getSecurities() {
    return Arrays.stream(CodefSecuritiesInstitution.values())
        .map(CodefSecuritiesInstitutionResponse::new)
        .collect(Collectors.toList());
  }
}
