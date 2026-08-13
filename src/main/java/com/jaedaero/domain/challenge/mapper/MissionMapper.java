package com.jaedaero.domain.challenge.mapper;

import com.jaedaero.domain.challenge.common.enums.MissionType;
import com.jaedaero.domain.challenge.vo.BadgeVo;
import com.jaedaero.domain.challenge.vo.InvestmentBadgeStatusVo;
import com.jaedaero.domain.challenge.vo.MissionVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MissionMapper {

  /** 오늘 노출되는 미션을 한 번의 조회로 가져옵니다. */
  List<MissionVo> findTodayMissions(@Param("userId") long userId);

  /** 활성 상태의 미션을 식별자로 조회합니다. */
  MissionVo findActiveMissionById(@Param("missionId") long missionId);

  /** 사용자의 미션 완료 이력을 저장합니다. */
  int insertCompletion(
      @Param("userId") long userId,
      @Param("missionId") long missionId,
      @Param("completionDate") LocalDate completionDate);

  /** 사용자의 투자 뱃지 집계 행을 생성합니다. */
  int initializeInvestmentBadge(@Param("userId") long userId);

  /** 안정형 미션 완료 수를 증가시킵니다. */
  int incrementSafeMissionCount(@Param("userId") long userId);

  /** 공격형 미션 완료 수를 증가시킵니다. */
  int incrementAggressiveMissionCount(@Param("userId") long userId);

  /** 사용자의 투자 뱃지 현황을 조회합니다. */
  InvestmentBadgeStatusVo findInvestmentBadgeStatus(@Param("userId") long userId);

  /** 활성 뱃지 마스터 목록을 조회합니다. */
  List<BadgeVo> findActiveBadges();

  /** 선택된 뱃지의 획득 이력을 한 번에 저장합니다. */
  int insertUserBadges(
      @Param("userId") long userId, @Param("badgeIds") List<Long> badgeIds);

  /** 사용자의 성향별 투자 뱃지 티어를 갱신합니다. */
  int updateInvestmentBadgeGrade(
      @Param("userId") long userId,
      @Param("missionType") MissionType missionType,
      @Param("grade") String grade);

  /** 월간 동기 랭킹 집계의 미션 완료 수를 증가시킵니다. */
  int incrementMonthlyChallengeMissionCount(@Param("userId") long userId);

  /** 전체 동기 랭킹 집계의 미션 완료 수를 증가시킵니다. */
  int incrementTotalChallengeMissionCount(@Param("userId") long userId);
}
