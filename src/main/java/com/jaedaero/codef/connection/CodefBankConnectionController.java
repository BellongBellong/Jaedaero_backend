package com.jaedaero.codef.connection;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "CODEF - 은행 연결")
@RestController
@RequestMapping("/api/v1/codef/connections")
public class CodefBankConnectionController {

    private final CodefConnectionService connectionService;

    public CodefBankConnectionController(CodefConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @ApiOperation(
            value = "선택한 은행 연결 및 계좌 동기화",
            notes = "최초 연결 시 Connected ID를 발급하고 암호화해 DB에 저장합니다. 이후 연결은 기존 Connected ID에 기관을 추가합니다. "
                    + "Connected ID와 실제 계좌번호는 프론트엔드에 반환하지 않습니다.")
    @PostMapping
    public CodefConnectionResponse connectBank(
            @Valid @RequestBody CodefBankConnectionCreateRequest request) {
        return connectionService.connect(request.getUserId(), request);
    }
}
