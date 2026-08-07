package com.jaedaero.domain.challenge.service.impl;

import com.jaedaero.domain.challenge.dto.ChallengeGroupResponse;
import com.jaedaero.domain.challenge.dto.ChallengeTopRankerResponse;
import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import com.jaedaero.domain.challenge.exception.ChallengeErrorCode;
import com.jaedaero.domain.challenge.exception.ChallengeException;
import com.jaedaero.domain.challenge.mapper.ChallengeGroupMapper;
import com.jaedaero.domain.challenge.service.ChallengeGroupService;
import com.jaedaero.domain.challenge.vo.ChallengeGroupVo;
import com.jaedaero.domain.challenge.vo.ChallengeRankingMemberVo;
import com.jaedaero.domain.challenge.vo.ChallengeRankingStatisticsVo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChallengeGroupServiceImpl implements ChallengeGroupService {

  private final ChallengeGroupMapper challengeGroupMapper;
  private final Clock applicationClock;

  /** 사용자의 입대월 동기 기간별 랭킹 정보를 조회합니다. */
  @Override
  @Transactional(readOnly = true)
  public ChallengeGroupResponse getChallengeGroup(
      long userId, RankingPeriod rankingPeriod, String yearMonth) {
    ChallengeGroupVo group = findChallengeGroup(userId);
    LocalDate resultMonth = resolveResultMonth(rankingPeriod, yearMonth);
    ChallengeRankingMemberVo myRanking =
        challengeGroupMapper.findMyRankingByGroupId(
            group.getGroupId(), userId, rankingPeriod, resultMonth);
    ChallengeRankingStatisticsVo statistics =
        challengeGroupMapper.findRankingStatisticsByGroupId(
            group.getGroupId(), rankingPeriod, resultMonth);

    if (myRanking == null || statistics == null) {
      throw new ChallengeException(
          ChallengeErrorCode.CHALLENGE_GROUP_NOT_FOUND, "동기 그룹 랭킹 정보를 찾을 수 없습니다.");
    }

    return ChallengeGroupResponse.builder()
        .groupId(group.getGroupId())
        .rankingPeriod(rankingPeriod)
        .cohortName(createCohortName(group))
        .soldierType(group.getSoldierType())
        .enlistmentYear(group.getEnlistmentYear())
        .enlistmentMonth(group.getEnlistmentMonth())
        .periodStartDate(createPeriodStartDate(group, rankingPeriod, resultMonth))
        .periodEndDate(createPeriodEndDate(rankingPeriod, resultMonth))
        .memberCount(statistics.getMemberCount())
        .topRankers(
            challengeGroupMapper
                .findTopRankersByGroupId(group.getGroupId(), rankingPeriod, resultMonth)
                .stream()
                .map(ChallengeTopRankerResponse::from)
                .toList())
        .myRankingNo(myRanking.getRankingNo())
        .myPercentile(calculatePercentile(myRanking.getRankingNo(), statistics.getMemberCount()))
        .myMissionCompletionCount(myRanking.getMissionCompletionCount())
        .groupAverageMissionCompletionCount(statistics.getAverageMissionCompletionCount())
        .bottomQuarterAverageMissionCompletionCount(
            statistics.getBottomQuarterAverageMissionCompletionCount())
        .topTenPercentThreshold(statistics.getTopTenPercentThreshold())
        .averageDifference(
            myRanking.getMissionCompletionCount() - statistics.getAverageMissionCompletionCount())
        .lastUpdatedAt(statistics.getLastUpdatedAt())
        .build();
  }

  /** 사용자가 속한 입대월 동기 그룹을 조회합니다. */
  private ChallengeGroupVo findChallengeGroup(long userId) {
    ChallengeGroupVo group = challengeGroupMapper.findGroupByUserId(userId);
    if (group == null) {
      throw new ChallengeException(
          ChallengeErrorCode.CHALLENGE_GROUP_NOT_FOUND, "참여 중인 동기 그룹을 찾을 수 없습니다.");
    }
    return group;
  }

  /** 입대 연도와 월로 동기 그룹 표시명을 생성합니다. */
  private String createCohortName(ChallengeGroupVo group) {
    return String.format("%02d년 %d월 입대 동기", group.getEnlistmentYear() % 100, group.getEnlistmentMonth());
  }

  /** 조회 기간에 맞는 랭킹 집계 시작일을 생성합니다. */
  private LocalDate createPeriodStartDate(
      ChallengeGroupVo group, RankingPeriod rankingPeriod, LocalDate resultMonth) {
    if (rankingPeriod == RankingPeriod.MONTHLY) {
      return resultMonth;
    }
    return YearMonth.of(group.getEnlistmentYear(), group.getEnlistmentMonth()).atDay(1);
  }

  /** 조회 기간에 맞는 랭킹 집계 종료일을 생성합니다. */
  private LocalDate createPeriodEndDate(RankingPeriod rankingPeriod, LocalDate resultMonth) {
    LocalDate today = LocalDate.now(applicationClock);
    if (rankingPeriod != RankingPeriod.MONTHLY) {
      return today;
    }
    return resultMonth.plusMonths(1).minusDays(1).isBefore(today)
        ? resultMonth.plusMonths(1).minusDays(1)
        : today;
  }

  /** 월별 랭킹 조회에 사용할 결과 월을 검증합니다. */
  private LocalDate resolveResultMonth(RankingPeriod rankingPeriod, String yearMonth) {
    if (rankingPeriod != RankingPeriod.MONTHLY) {
      return null;
    }

    YearMonth resultYearMonth;
    try {
      resultYearMonth =
          yearMonth == null || yearMonth.isBlank()
              ? YearMonth.now(applicationClock)
              : YearMonth.parse(yearMonth);
    } catch (RuntimeException exception) {
      throw new ChallengeException(
          ChallengeErrorCode.INVALID_RANKING_MONTH, "조회 월은 yyyy-MM 형식이어야 합니다.");
    }

    if (resultYearMonth.isAfter(YearMonth.now(applicationClock))) {
      throw new ChallengeException(
          ChallengeErrorCode.INVALID_RANKING_MONTH, "미래 월의 랭킹은 조회할 수 없습니다.");
    }
    return resultYearMonth.atDay(1);
  }

  /** 순위와 참여 인원 수를 기준으로 상위 백분율을 계산합니다. */
  private int calculatePercentile(int rankingNo, int memberCount) {
    if (memberCount <= 0) {
      return 0;
    }
    return (int) Math.ceil((double) rankingNo * 100 / memberCount);
  }
}
