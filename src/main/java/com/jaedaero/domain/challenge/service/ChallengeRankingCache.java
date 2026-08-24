package com.jaedaero.domain.challenge.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import com.jaedaero.domain.challenge.vo.ChallengeRankingMemberVo;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** 동기 그룹 순위의 반복 재계산을 막는 짧은 수명 메모리 캐시입니다. */
@Component
public class ChallengeRankingCache {

  private final Cache<Key, Snapshot> snapshots =
      Caffeine.newBuilder().maximumSize(256).expireAfterWrite(Duration.ofSeconds(10)).build();

  public Snapshot get(
      long groupId,
      RankingPeriod rankingPeriod,
      LocalDate resultMonth,
      Function<Key, Snapshot> snapshotLoader) {
    Key key = new Key(groupId, rankingPeriod, resultMonth);
    return snapshots.get(key, snapshotLoader);
  }

  public void invalidateGroup(long groupId) {
    snapshots.asMap().keySet().removeIf(key -> key.groupId() == groupId);
  }

  public record Key(long groupId, RankingPeriod rankingPeriod, LocalDate resultMonth) {}

  public record Snapshot(
      List<ChallengeRankingMemberVo> rankedMembers,
      Map<Long, ChallengeRankingMemberVo> membersByUserId,
      int averageMissionCompletionCount,
      int bottomQuarterAverageMissionCompletionCount,
      int topTenPercentThreshold,
      LocalDateTime lastUpdatedAt) {}
}
