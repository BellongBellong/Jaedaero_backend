package com.jaedaero.domain.challenge.service;

import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import com.jaedaero.domain.challenge.dto.ChallengeGroupResponse;

public interface ChallengeGroupService {

  /** 사용자의 입대월 동기 기간별 랭킹 정보를 조회합니다. */
  ChallengeGroupResponse getChallengeGroup(long userId, RankingPeriod rankingPeriod, String yearMonth);

  /** 미션 완료 후 사용자가 속한 동기 그룹의 랭킹 캐시를 무효화합니다. */
  void invalidateRankingCache(long userId);
}
