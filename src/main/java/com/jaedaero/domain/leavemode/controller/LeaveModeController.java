package com.jaedaero.domain.leavemode.controller;

import com.jaedaero.domain.leavemode.dto.LeaveModeRequest;
import com.jaedaero.domain.leavemode.dto.LeaveModeBudgetRequest;
import com.jaedaero.domain.leavemode.dto.LeaveModeResponse;
import com.jaedaero.domain.leavemode.exception.LeaveModeErrorCode;
import com.jaedaero.domain.leavemode.exception.LeaveModeException;
import com.jaedaero.domain.leavemode.service.LeaveModeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;
import javax.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "휴가모드")
@RestController
@RequestMapping("/api/v1/leave-mode")
@RequiredArgsConstructor
public class LeaveModeController {

  private final LeaveModeService leaveModeService;

  @ApiOperation(value = "휴가모드 시작", notes = "휴가·외출·외박 일정을 저장하고 해당 기간에 휴가모드를 활성화합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "휴가모드 일정 저장", response = LeaveModeResponse.class),
    @ApiResponse(code = 400, message = "요청 값이 올바르지 않음"),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  @PostMapping
  public ResponseEntity<LeaveModeResponse> startLeaveMode(
      @ApiIgnore Authentication authentication, @Valid @RequestBody LeaveModeRequest request) {
    return ResponseEntity.ok(
        leaveModeService.startLeaveMode(authenticatedUserId(authentication), request));
  }

  @ApiOperation(value = "현재 휴가모드 상태 조회", notes = "오늘 활성화된 휴가모드 일정이 없으면 204를 반환합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "휴가모드 활성", response = LeaveModeResponse.class),
    @ApiResponse(code = 204, message = "활성 휴가모드 없음"),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  @GetMapping("/current")
  public ResponseEntity<LeaveModeResponse> getCurrentLeaveMode(
      @ApiIgnore Authentication authentication) {
    LeaveModeResponse response = leaveModeService.getCurrentLeaveMode(authenticatedUserId(authentication));
    return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
  }

  @ApiOperation(value = "휴가모드 일정 목록 조회", notes = "삭제되지 않은 사용자의 이벤트 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "이벤트 목록 조회", response = LeaveModeResponse.class),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  @GetMapping
  public ResponseEntity<List<LeaveModeResponse>> getLeaveModes(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(leaveModeService.getLeaveModes(authenticatedUserId(authentication)));
  }

  @ApiOperation(value = "휴가 예산 변경", notes = "현재 사용자의 휴가 일정에 설정한 예산을 변경하거나 삭제합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "휴가 예산 변경", response = LeaveModeResponse.class),
    @ApiResponse(code = 400, message = "요청 값이 올바르지 않음"),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "휴가 일정을 찾을 수 없음")
  })
  @PutMapping("/{leaveModeId}/budget")
  public ResponseEntity<LeaveModeResponse> updateBudget(
      @ApiIgnore Authentication authentication,
      @PathVariable long leaveModeId,
      @Valid @RequestBody LeaveModeBudgetRequest request) {
    return ResponseEntity.ok(
        leaveModeService.updateBudget(authenticatedUserId(authentication), leaveModeId, request));
  }

  @ApiOperation(value = "휴가모드 일정 삭제", notes = "사용자의 이벤트를 삭제 처리합니다.")
  @ApiResponses({
    @ApiResponse(code = 204, message = "이벤트 삭제"),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "이벤트를 찾을 수 없음")
  })
  @DeleteMapping("/{leaveModeId}")
  public ResponseEntity<Void> deleteLeaveMode(
      @ApiIgnore Authentication authentication, @PathVariable long leaveModeId) {
    leaveModeService.deleteLeaveMode(authenticatedUserId(authentication), leaveModeId);
    return ResponseEntity.noContent().build();
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new LeaveModeException(LeaveModeErrorCode.AUTHENTICATION_REQUIRED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new LeaveModeException(
          LeaveModeErrorCode.AUTHENTICATION_REQUIRED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
