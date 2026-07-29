package com.jaedaero.domain.codef.connection;

import com.jaedaero.domain.codef.institution.CodefBusinessType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "CODEF 금융기관 연결")
@RestController
@RequestMapping("/api/v1/codef/connections")
public class CodefBankConnectionController {

  private final CodefConnectionService connectionService;

  public CodefBankConnectionController(CodefConnectionService connectionService) {
    this.connectionService = connectionService;
  }

  @ApiOperation(
      value = "선택한 은행 연결 및 계좌 동기화",
      notes =
          "최초 연결 시 CODEF 연결 식별값을 발급하고 암호화하여 저장합니다. 이후 요청은 기존 연결에 은행을 추가합니다. "
              + "연결 식별값과 실제 계좌번호는 응답에 포함하지 않습니다.")
  @PostMapping
  public CodefConnectionResponse connectBank(
      @Valid @RequestBody CodefBankConnectionCreateRequest request) {
    return connectionService.connect(request.getUserId(), request);
  }

  @ApiOperation(
      value = "증권사 연결 및 증권 계좌 동기화",
      notes =
          "증권사 기관 코드와 온라인 거래 인증정보를 입력합니다. 증권 업무 구분과 고객 구분은 서버에서 자동으로 설정합니다. "
              + "예: 미래에셋증권(0238), 키움증권(0264)")
  @PostMapping("/securities")
  public CodefConnectionResponse connectSecurities(
      @Valid @RequestBody CodefBankConnectionCreateRequest request) {
    request.setBusinessType(CodefBusinessType.SECURITIES.getCode());
    return connectionService.connect(request.getUserId(), request);
  }
}
