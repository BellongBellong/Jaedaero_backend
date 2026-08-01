package com.jaedaero.domain.auth.mapper;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.auth.vo.AuthUserVo;
import java.sql.Timestamp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

  /** 소셜 로그인 식별자로 활성 사용자를 조회합니다. */
  AuthUserVo findActiveBySocialIdentity(
      @Param("socialType") String socialType, @Param("socialId") String socialId);

  /** 현재 사용자를 제외한 닉네임 사용 건수를 조회합니다. */
  int availabilityNickname(
      @Param("nickname") String nickname, @Param("userId") long userId);

  /** 사용자의 닉네임을 변경합니다. */
  int updateNickname(@Param("userId") long userId, @Param("nickname") String nickname);

  /** 사용자의 프로필 아이콘과 배경색을 변경합니다. */
  int updateProfileAppearance(
      @Param("userId") long userId,
      @Param("profileImage") ProfileImage profileImage,
      @Param("profileSource") ProfileSource profileSource);

  /** 활성 사용자 존재 여부를 조회합니다. */
  int countActiveByUserId(@Param("userId") long userId);

  /** 사용자 약관 동의 이력을 기록합니다. */
  void insertAgreement(
      @Param("userId") long userId,
      @Param("agreementType") String agreementType,
      @Param("agreementVersion") String agreementVersion,
      @Param("required") boolean required);

  /** 신규 소셜 사용자를 생성합니다. */
  void insert(@Param("socialType") String socialType, @Param("socialId") String socialId);

  /** 유효한 리프레시 토큰 해시로 활성 사용자를 조회합니다. */
  AuthUserVo findActiveByRefreshTokenHash(@Param("tokenHash") String tokenHash);

  /** 리프레시 토큰 해시를 저장합니다. */
  void insertRefreshToken(
      @Param("userId") long userId,
      @Param("tokenHash") String tokenHash,
      @Param("expiresAt") Timestamp expiresAt);

  /** 리프레시 토큰 해시를 삭제합니다. */
  void deleteRefreshTokenByHash(@Param("tokenHash") String tokenHash);

  /** 사용자의 모든 리프레시 토큰을 삭제합니다. */
  void deleteRefreshTokensByUserId(@Param("userId") long userId);
}
