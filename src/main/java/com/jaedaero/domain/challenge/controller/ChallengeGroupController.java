package com.jaedaero.domain.challenge.controller;

import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import com.jaedaero.domain.challenge.dto.ChallengeGroupResponse;
import com.jaedaero.domain.challenge.exception.ChallengeErrorCode;
import com.jaedaero.domain.challenge.exception.ChallengeException;
import com.jaedaero.domain.challenge.service.ChallengeGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "챌린지")
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeGroupController {

  private final ChallengeGroupService challengeGroupService;

  @ApiOperation(
      value = "입대월 동기 그룹 랭킹 조회",
      notes = "period 값으로 누적 또는 월별 미션 완료 수 기준 랭킹을 조회합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = ChallengeGroupResponse.class),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 400, message = "잘못된 조회 월"),
    @ApiResponse(code = 404, message = "동기 그룹을 찾을 수 없음")
  })
  @GetMapping("/group")
  public ResponseEntity<ChallengeGroupResponse> getChallengeGroup(
      @ApiIgnore Authentication authentication,
      @ApiParam(value = "랭킹 기간", allowableValues = "CUMULATIVE,MONTHLY", example = "CUMULATIVE")
          @RequestParam(defaultValue = "CUMULATIVE") RankingPeriod period,
      @ApiParam(value = "월별 랭킹 조회 월", example = "2026-08")
          @RequestParam(required = false) String yearMonth) {
    return ResponseEntity.ok(
        challengeGroupService.getChallengeGroup(
            authenticatedUserId(authentication), period, yearMonth));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new ChallengeException(ChallengeErrorCode.AUTHENTICATION_REQUIRED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new ChallengeException(
          ChallengeErrorCode.AUTHENTICATION_REQUIRED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
