package com.jaedaero.domain.goal.controller;

import com.jaedaero.domain.goal.dto.GoalRequest;
import com.jaedaero.domain.goal.dto.GoalResponse;
import com.jaedaero.domain.goal.service.GoalService;
import com.jaedaero.domain.mypage.exception.MyPageErrorCode;
import com.jaedaero.domain.mypage.exception.MyPageException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Api(tags = "목표")
public class GoalController {
  private final GoalService goalService;

  @ApiOperation(value = "목표 조회")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = GoalResponse.class),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "목표 정보를 찾을 수 없음")
  })
  @GetMapping
  public ResponseEntity<GoalResponse> getGoal(@ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(goalService.getGoal(getAuthenticatedUserId(authentication)));
  }

  @PutMapping
  @ApiOperation(value = "목표 금액 변경")
  @ApiResponses({
    @ApiResponse(code = 200, message = "변경 성공", response = GoalResponse.class),
    @ApiResponse(code = 400, message = "유효하지 않은 목표 금액"),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "목표 정보를 찾을 수 없음")
  })
  public ResponseEntity<GoalResponse> updateGoal(
      @ApiIgnore Authentication authentication, @Valid @RequestBody GoalRequest request) {
    return ResponseEntity.ok(goalService.updateGoal(getAuthenticatedUserId(authentication), request));
  }

  private long getAuthenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new MyPageException(MyPageErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new MyPageException(MyPageErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
  }
}
