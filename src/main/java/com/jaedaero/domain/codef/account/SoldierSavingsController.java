package com.jaedaero.domain.codef.account;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "장병내일준비적금")
@RequestMapping("/api/v1/soldier-savings")
public class SoldierSavingsController {

  private final SoldierSavingsService soldierSavingsService;

  public SoldierSavingsController(SoldierSavingsService soldierSavingsService) {
    this.soldierSavingsService = soldierSavingsService;
  }

  @ApiOperation(
      value = "장병내일준비적금 보유 여부 조회",
      notes = "이미 CODEF에서 동기화한 계좌목록의 상품명으로 판별합니다. 이 API는 CODEF를 다시 호출하지 않습니다.")
  @GetMapping
  public SoldierSavingsResponse getSoldierSavings(
      @ApiParam(value = "사용자 ID", required = true, example = "1") @RequestParam long userId) {
    return soldierSavingsService.getSoldierSavings(userId);
  }
}
