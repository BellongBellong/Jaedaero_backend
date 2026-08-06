package com.jaedaero.domain.challenge.mapper;

import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import com.jaedaero.domain.challenge.vo.ChallengeGroupVo;
import com.jaedaero.domain.challenge.vo.ChallengeRankingMemberVo;
import com.jaedaero.domain.challenge.vo.ChallengeRankingStatisticsVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChallengeGroupMapper {

  /** 사용자가 속한 입대월 동기 그룹을 조회합니다. */
  ChallengeGroupVo findGroupByUserId(@Param("userId") long userId);

  /** 요청 기간의 미션 완료 수 기준 동기 그룹 상위 3명을 조회합니다. */
  List<ChallengeRankingMemberVo> findTopRankersByGroupId(
      @Param("groupId") long groupId,
      @Param("rankingPeriod") RankingPeriod rankingPeriod,
      @Param("resultMonth") LocalDate resultMonth);

  /** 요청 기간의 미션 완료 수 기준 사용자 순위와 완료 수를 조회합니다. */
  ChallengeRankingMemberVo findMyRankingByGroupId(
      @Param("groupId") long groupId,
      @Param("userId") long userId,
      @Param("rankingPeriod") RankingPeriod rankingPeriod,
      @Param("resultMonth") LocalDate resultMonth);

  /** 요청 기간의 동기 그룹 랭킹 비교 통계를 조회합니다. */
  ChallengeRankingStatisticsVo findRankingStatisticsByGroupId(
      @Param("groupId") long groupId,
      @Param("rankingPeriod") RankingPeriod rankingPeriod,
      @Param("resultMonth") LocalDate resultMonth);
}
