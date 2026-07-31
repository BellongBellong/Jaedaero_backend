package com.jaedaero.domain.auth.mapper;

import com.jaedaero.domain.auth.vo.AuthUserVo;
import java.sql.Timestamp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

  AuthUserVo findActiveBySocialIdentity(
      @Param("socialType") String socialType, @Param("socialId") String socialId);

  int availabilityNickname(
      @Param("nickname") String nickname, @Param("userId") long userId);

  int updateNickname(@Param("userId") long userId, @Param("nickname") String nickname);

  void insert(@Param("socialType") String socialType, @Param("socialId") String socialId);

  AuthUserVo findActiveByRefreshTokenHash(@Param("tokenHash") String tokenHash);

  void insertRefreshToken(
      @Param("userId") long userId,
      @Param("tokenHash") String tokenHash,
      @Param("expiresAt") Timestamp expiresAt);

  void deleteRefreshTokenByHash(@Param("tokenHash") String tokenHash);

  void deleteRefreshTokensByUserId(@Param("userId") long userId);
}
