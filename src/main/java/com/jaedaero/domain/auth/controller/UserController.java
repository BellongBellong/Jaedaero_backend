package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.NicknameAvailabilityResponse;
import com.jaedaero.domain.auth.dto.NicknameRequest;
import com.jaedaero.domain.auth.dto.ProfileAppearanceRequest;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.NicknameException;
import com.jaedaero.domain.auth.service.NicknameService;
import com.jaedaero.domain.auth.service.ProfileAppearanceService;
import com.jaedaero.domain.mypage.dto.MyPageProfileResponse;
import com.jaedaero.domain.mypage.dto.InvestmentBadgeResponse;
import com.jaedaero.domain.mypage.service.MyPageService;
import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Api(tags = "회원가입")
public class UserController {

  private final NicknameService nicknameService;
  private final ProfileAppearanceService profileAppearanceService;
  private final MyPageService myPageService;

  @GetMapping("/me")
  @ApiOperation(value = "마이페이지 회원 정보 조회")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = MyPageProfileResponse.class),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "사용자를 찾을 수 없음")
  })
  public ResponseEntity<MyPageProfileResponse> getMyPageProfile(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(myPageService.getProfile(getAuthenticatedUserId(authentication)));
  }

  @GetMapping("/investment-badges")
  @ApiOperation(value = "투자 뱃지 획득 내역 조회")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = InvestmentBadgeResponse.class),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<List<InvestmentBadgeResponse>> getInvestmentBadges(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(
        myPageService.getInvestmentBadges(getAuthenticatedUserId(authentication)));
  }

  @ApiOperation(value = "금융기관 연동 해지", notes = "현재 사용자의 CODEF 금융기관과 연결 계좌를 해지 상태로 변경합니다.")
  @ApiResponses({
    @ApiResponse(code = 204, message = "연동 해지 성공"),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "활성 금융기관 연동 정보를 찾을 수 없음")
  })
  @DeleteMapping("/codef/unlink")
  public ResponseEntity<Void> unlinkCodef(@ApiIgnore Authentication authentication) {
    myPageService.unlinkCodef(getAuthenticatedUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/me")
  @ApiOperation(value = "회원 탈퇴")
  @ApiResponses({
    @ApiResponse(code = 204, message = "탈퇴 성공"),
    @ApiResponse(code = 401, message = "인증 필요"),
    @ApiResponse(code = 404, message = "사용자를 찾을 수 없음")
  })
  public ResponseEntity<Void> withdraw(@ApiIgnore Authentication authentication) {
    myPageService.withdraw(getAuthenticatedUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/nickname/availability")
  @ApiOperation(value = "닉네임 중복 확인", notes = "현재 사용자를 제외하고 닉네임 사용 가능 여부를 확인합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "확인 성공", response = NicknameAvailabilityResponse.class),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<NicknameAvailabilityResponse> checkNicknameAvailability(
      @ApiIgnore Authentication authentication,
      @RequestParam @NotBlank @Size(max = 12) @Pattern(regexp = "[가-힣a-zA-Z0-9]{2,12}")
          String nickname) {
    long userId = getAuthenticatedUserId(authentication);
    return ResponseEntity.ok(
        new NicknameAvailabilityResponse(nicknameService.isAvailable(userId, nickname)));
  }

  @PutMapping("/nickname")
  @ApiOperation(value = "닉네임 설정 또는 변경")
  @ApiResponses({
    @ApiResponse(code = 204, message = "변경 성공"),
    @ApiResponse(code = 400, message = "형식이 올바르지 않은 닉네임"),
    @ApiResponse(code = 409, message = "이미 사용 중인 닉네임")
  })
  public ResponseEntity<Void> updateNickname(
      @ApiIgnore Authentication authentication, @Valid @RequestBody NicknameRequest request) {
    nicknameService.updateNickname(getAuthenticatedUserId(authentication), request.getNickname());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/profile-appearance")
  @ApiOperation(value = "프로필 아이콘과 배경색 설정")
  @ApiResponses({
    @ApiResponse(code = 204, message = "변경 성공"),
    @ApiResponse(code = 400, message = "프로필 아이콘 또는 배경색 누락"),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<Void> updateProfileAppearance(
      @ApiIgnore Authentication authentication,
      @Valid @RequestBody ProfileAppearanceRequest request) {
    profileAppearanceService.updateProfileAppearance(
        getAuthenticatedUserId(authentication), request.getProfileImage(), request.getProfileSource());
    return ResponseEntity.noContent().build();
  }

  private long getAuthenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new NicknameException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }

    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new NicknameException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
  }
}
