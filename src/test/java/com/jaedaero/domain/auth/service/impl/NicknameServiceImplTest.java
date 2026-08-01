package com.jaedaero.domain.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.NicknameException;
import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.auth.vo.AuthUserVo;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NicknameServiceImplTest {

  @Test
  void returnsTrueWhenNicknameIsNotInUse() {
    NicknameServiceImpl service = new NicknameServiceImpl(new StubAuthUserMapper());

    assertTrue(service.isAvailable(1L, "jaedaero"));
  }

  @Test
  void returnsFalseWhenNicknameIsInUseByAnotherUser() {
    StubAuthUserMapper mapper = new StubAuthUserMapper();
    mapper.occupiedNicknames.add("jaedaero");
    NicknameServiceImpl service = new NicknameServiceImpl(mapper);

    assertFalse(service.isAvailable(1L, "jaedaero"));
  }

  @Test
  void updatesTrimmedNickname() {
    StubAuthUserMapper mapper = new StubAuthUserMapper();
    NicknameServiceImpl service = new NicknameServiceImpl(mapper);

    service.updateNickname(1L, "  jaedaero  ");

    assertEquals("jaedaero", mapper.updatedNickname);
  }

  @Test
  void throwsConflictWhenNicknameIsAlreadyInUse() {
    StubAuthUserMapper mapper = new StubAuthUserMapper();
    mapper.occupiedNicknames.add("jaedaero");
    NicknameServiceImpl service = new NicknameServiceImpl(mapper);

    NicknameException exception =
        assertThrows(NicknameException.class, () -> service.updateNickname(1L, "jaedaero"));

    assertEquals(AuthErrorCode.NICKNAME_ALREADY_IN_USE, exception.getErrorCode());
  }

  @Test
  void throwsNotFoundWhenActiveUserDoesNotExist() {
    StubAuthUserMapper mapper = new StubAuthUserMapper();
    mapper.userExists = false;
    NicknameServiceImpl service = new NicknameServiceImpl(mapper);

    NicknameException exception =
        assertThrows(NicknameException.class, () -> service.updateNickname(1L, "jaedaero"));

    assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void throwsBadRequestForBlankInvalidOrTooLongNickname() {
    NicknameServiceImpl service = new NicknameServiceImpl(new StubAuthUserMapper());

    NicknameException blankException =
        assertThrows(NicknameException.class, () -> service.updateNickname(1L, "   "));
    NicknameException tooLongException =
        assertThrows(NicknameException.class, () -> service.updateNickname(1L, "a".repeat(13)));
    NicknameException invalidCharacterException =
        assertThrows(NicknameException.class, () -> service.updateNickname(1L, "jaedaero1"));

    assertEquals(AuthErrorCode.INVALID_NICKNAME, blankException.getErrorCode());
    assertEquals(AuthErrorCode.INVALID_NICKNAME, tooLongException.getErrorCode());
    assertEquals(AuthErrorCode.INVALID_NICKNAME, invalidCharacterException.getErrorCode());
  }

  private static class StubAuthUserMapper implements AuthUserMapper {

    private final Set<String> occupiedNicknames = new HashSet<>();
    private boolean userExists = true;
    private String updatedNickname;

    @Override
    public AuthUserVo findActiveBySocialIdentity(String socialType, String socialId) {
      return null;
    }

    @Override
    public int availabilityNickname(String nickname, long userId) {
      return occupiedNicknames.contains(nickname) ? 1 : 0;
    }

    @Override
    public int updateNickname(long userId, String nickname) {
      if (!userExists) {
        return 0;
      }
      updatedNickname = nickname;
      return 1;
    }

    @Override
    public int updateProfileAppearance(
        long userId, ProfileImage profileImage, ProfileSource profileSource) {
      return userExists ? 1 : 0;
    }

    @Override
    public int countActiveByUserId(long userId) {
      return userExists ? 1 : 0;
    }

    @Override
    public void insertAgreement(
        long userId, String agreementType, String agreementVersion, boolean required) {}

    @Override
    public void insert(String socialType, String socialId) {}

    @Override
    public AuthUserVo findActiveByRefreshTokenHash(String tokenHash) {
      return null;
    }

    @Override
    public void insertRefreshToken(long userId, String tokenHash, Timestamp expiresAt) {}

    @Override
    public void deleteRefreshTokenByHash(String tokenHash) {}

    @Override
    public void deleteRefreshTokensByUserId(long userId) {}
  }
}
