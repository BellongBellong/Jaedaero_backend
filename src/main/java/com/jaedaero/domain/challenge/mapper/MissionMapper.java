package com.jaedaero.domain.challenge.mapper;

import com.jaedaero.domain.challenge.common.enums.MissionType;
import com.jaedaero.domain.challenge.vo.InvestmentBadgeStatusVo;
import com.jaedaero.domain.challenge.vo.MissionVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MissionMapper {

  /** 오늘 노출할 공통 데일리 미션을 조회합니다. */
  List<MissionVo> findDailyMissions(@Param("userId") long userId);

  /** 오늘 노출할 성향별 추천 미션을 조회합니다. */
  MissionVo findRecommendedMission(
      @Param("userId") long userId, @Param("missionType") MissionType missionType);

  /** 오늘 조건을 충족한 이벤트 미션을 조회합니다. */
  MissionVo findEventMission(@Param("userId") long userId);

  /** 활성 상태의 미션을 식별자로 조회합니다. */
  MissionVo findActiveMissionById(@Param("missionId") long missionId);

  /** 사용자의 당일 미션 완료 여부를 조회합니다. */
  boolean existsCompletionToday(@Param("userId") long userId, @Param("missionId") long missionId);

  /** 사용자의 미션 완료 이력 존재 여부를 조회합니다. */
  boolean existsAnyCompletion(@Param("userId") long userId, @Param("missionId") long missionId);

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

  /** 성향과 완료 수에 해당하는 최고 티어를 조회합니다. */
  String findGradeByCompletionCount(
      @Param("missionType") MissionType missionType,
      @Param("completionCount") int completionCount);

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
