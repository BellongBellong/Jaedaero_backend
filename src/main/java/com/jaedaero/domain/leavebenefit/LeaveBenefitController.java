package com.jaedaero.domain.leavebenefit;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/benefits")
@Api(tags = "휴가 혜택")
public class LeaveBenefitController {

  private final LeaveBenefitService leaveBenefitService;

  public LeaveBenefitController(LeaveBenefitService leaveBenefitService) {
    this.leaveBenefitService = leaveBenefitService;
  }

  @GetMapping
  @ApiOperation(
      value = "휴가 혜택 목록 조회",
      notes = "현재 날짜에 유효한 휴가 혜택만 반환합니다. 계급과 무관하게 현역 장병 공통 혜택을 조회합니다.")
  public ResponseEntity<List<LeaveBenefitResponse>> getLeaveBenefits(
      @ApiParam(
              value = "휴가 혜택 카테고리",
              example = "LEISURE",
              allowableValues = "TRANSPORT,LEISURE,SELF_DEVELOPMENT,LODGING,ETC")
          @RequestParam(required = false)
          LeaveBenefitCategory category) {
    return ResponseEntity.ok(leaveBenefitService.getAvailableBenefits(category));
  }
}
