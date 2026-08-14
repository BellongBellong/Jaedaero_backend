package com.jaedaero.domain.auth.mapper;

import com.jaedaero.domain.auth.vo.SoldierProfileVo;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MilitaryInfoMapper {

  /** 활성 사용자 존재 여부를 조회합니다. */
  int countActiveUserByUserId(@Param("userId") long userId);

  /** 장병내일준비적금 가입 여부를 조회합니다. */
  int countSoldierSavingByUserId(@Param("userId") long userId);

  /** 군 복무 프로필을 등록하거나 갱신합니다. */
  void upsertSoldierProfile(SoldierProfileVo soldierProfile);

  /** 입대일 기준 현재 계급을 갱신할 복무 프로필을 조회합니다. */
  List<SoldierProfileVo> findProfilesForRankRefresh();

  /** 사용자 입력이 아닌 배치 계산 결과로 현재 계급만 갱신합니다. */
  int updateRankName(@Param("userId") long userId, @Param("rankName") String rankName);

  /** 입대 동기 챌린지 그룹을 생성합니다. */
  void insertChallengeGroupIgnore(
      @Param("soldierType") String soldierType,
      @Param("enlistmentYear") int enlistmentYear,
      @Param("enlistmentMonth") int enlistmentMonth);

  /** 군종과 입대 월로 챌린지 그룹 식별자를 조회합니다. */
  Long findChallengeGroupId(
      @Param("soldierType") String soldierType,
      @Param("enlistmentYear") int enlistmentYear,
      @Param("enlistmentMonth") int enlistmentMonth);

  /** 사용자를 챌린지 그룹에 연결합니다. */
  void insertChallengeMemberIgnore(
      @Param("groupId") long groupId, @Param("userId") long userId);

  /** 챌린지 그룹 사용자의 목표 금액 평균을 조회합니다. */
  BigDecimal findChallengeGroupTargetAmountAverage(@Param("groupId") long groupId);
}
