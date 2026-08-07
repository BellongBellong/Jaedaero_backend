package com.jaedaero.domain.challenge.controller;

import com.jaedaero.domain.challenge.dto.MissionCompletionResponse;
import com.jaedaero.domain.challenge.dto.MissionResponse;
import com.jaedaero.domain.challenge.exception.ChallengeErrorCode;
import com.jaedaero.domain.challenge.exception.ChallengeException;
import com.jaedaero.domain.challenge.service.MissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "챌린지")
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

  private final MissionService missionService;

  @ApiOperation(value = "오늘의 미션 목록 조회", notes = "공통 데일리, 성향별 추천, 조건형 이벤트 미션을 조회합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = MissionResponse.class),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  @GetMapping("/today")
  public ResponseEntity<List<MissionResponse>> getTodayMissions(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(missionService.getTodayMissions(authenticatedUserId(authentication)));
  }

  @ApiOperation(value = "미션 완료", notes = "미션 완료 이력과 성향별 뱃지 및 챌린지 집계를 갱신합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "미션 완료", response = MissionCompletionResponse.class),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 403, message = "오늘 완료할 수 없는 미션"),
    @ApiResponse(code = 404, message = "미션을 찾을 수 없음"),
    @ApiResponse(code = 409, message = "이미 완료한 미션")
  })
  @PostMapping("/{missionId}/complete")
  public ResponseEntity<MissionCompletionResponse> completeMission(
      @ApiIgnore Authentication authentication,
      @ApiParam(value = "완료할 미션 ID", required = true, example = "1") @PathVariable long missionId) {
    return ResponseEntity.ok(
        missionService.completeMission(authenticatedUserId(authentication), missionId));
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
