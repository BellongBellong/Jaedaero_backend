package com.jaedaero.domain.challenge.service.impl;
import com.jaedaero.domain.challenge.common.enums.MissionType;
import com.jaedaero.domain.challenge.common.enums.RankingPeriod;
import com.jaedaero.domain.challenge.dto.MissionCompletionResponse;
import com.jaedaero.domain.challenge.dto.MissionResponse;
import com.jaedaero.domain.challenge.exception.ChallengeErrorCode;
import com.jaedaero.domain.challenge.exception.ChallengeException;
import com.jaedaero.domain.challenge.mapper.MissionMapper;
import com.jaedaero.domain.challenge.service.ChallengeGroupService;
import com.jaedaero.domain.challenge.mapper.ChallengeGroupMapper;
import com.jaedaero.domain.challenge.service.MissionService;
import com.jaedaero.domain.challenge.vo.BadgeVo;
import com.jaedaero.domain.challenge.vo.ChallengeGroupVo;
import com.jaedaero.domain.challenge.vo.ChallengeRankingMemberVo;
import com.jaedaero.domain.challenge.vo.InvestmentBadgeStatusVo;
import com.jaedaero.domain.challenge.vo.MissionVo;
import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.service.NotificationCommandService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissionServiceImpl implements MissionService {

  private static final int DEADLOCK_RETRY_MAX_ATTEMPTS = 5;

  private final MissionMapper missionMapper;
  private final ChallengeGroupService challengeGroupService;
  private final ChallengeGroupMapper challengeGroupMapper;
  private final NotificationCommandService notificationCommandService;
  private final Clock applicationClock;
  private final TransactionTemplate missionCompletionTransaction;

  public MissionServiceImpl(
      MissionMapper missionMapper,
      ChallengeGroupService challengeGroupService,
      ChallengeGroupMapper challengeGroupMapper,
      NotificationCommandService notificationCommandService,
      Clock applicationClock,
      PlatformTransactionManager transactionManager) {
    this.missionMapper = missionMapper;
    this.challengeGroupService = challengeGroupService;
    this.challengeGroupMapper = challengeGroupMapper;
    this.notificationCommandService = notificationCommandService;
    this.applicationClock = applicationClock;
    this.missionCompletionTransaction = new TransactionTemplate(transactionManager);
    this.missionCompletionTransaction.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** 오늘 사용자에게 노출할 미션 목록을 조회합니다. */
  @Override
  @Transactional(readOnly = true)
  public List<MissionResponse> getTodayMissions(long userId) {
    return findTodayMissionVos(userId).stream().map(MissionResponse::from).toList();
  }

  /** 사용자의 미션 완료를 처리하고 뱃지 및 랭킹 집계를 갱신합니다. */
  @Override
  public MissionCompletionResponse completeMission(long userId, long missionId) {
    for (int attempt = 1; attempt <= DEADLOCK_RETRY_MAX_ATTEMPTS; attempt++) {
      try {
        return missionCompletionTransaction.execute(
            status -> completeMissionInTransaction(userId, missionId));
      } catch (DeadlockLoserDataAccessException exception) {
        if (attempt == DEADLOCK_RETRY_MAX_ATTEMPTS) {
          throw new ChallengeException(
              ChallengeErrorCode.MISSION_COMPLETION_RETRY_EXHAUSTED,
              "미션 완료 처리 중 일시적인 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
        waitBeforeDeadlockRetry(attempt);
      }
    }
    throw new IllegalStateException("미션 완료 재시도 횟수를 확인할 수 없습니다.");
  }

  private MissionCompletionResponse completeMissionInTransaction(long userId, long missionId) {
    MissionVo mission =
        findTodayMissionVos(userId).stream()
            .filter(todayMission -> todayMission.getMissionId() == missionId)
            .findFirst()
            .orElse(null);
    if (mission == null) {
      if (missionMapper.findActiveMissionById(missionId) == null) {
        throw new ChallengeException(ChallengeErrorCode.MISSION_NOT_FOUND, "활성 미션을 찾을 수 없습니다.");
      }
      throw new ChallengeException(ChallengeErrorCode.MISSION_NOT_AVAILABLE, "오늘 완료할 수 없는 미션입니다.");
    }
    if (mission.isCompleted()) {
      throw new ChallengeException(ChallengeErrorCode.MISSION_ALREADY_COMPLETED, "이미 완료한 미션입니다.");
    }

    LocalDate completionDate = LocalDate.now(applicationClock);
    ChallengeGroupVo challengeGroup = challengeGroupMapper.findGroupByUserId(userId);
    ChallengeRankingMemberVo previousMonthlyRanking =
        findCurrentMonthlyRanking(userId, challengeGroup, completionDate);
    missionMapper.insertCompletion(userId, missionId, completionDate);

    InvestmentBadgeStatusVo badgeStatus = updateInvestmentBadge(userId, mission.getMissionType());
    missionMapper.incrementMonthlyChallengeMissionCount(userId);
    missionMapper.incrementTotalChallengeMissionCount(userId);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            challengeGroupService.invalidateRankingCache(userId);
          }
        });
    ChallengeRankingMemberVo currentMonthlyRanking =
        findCurrentMonthlyRanking(userId, challengeGroup, completionDate);
    int monthlyMissionCount =
        currentMonthlyRanking == null ? 0 : currentMonthlyRanking.getMissionCompletionCount();

    notificationCommandService.createUserNotification(
        userId,
        NotificationType.MISSION_COMPLETED,
        "미션 달성",
        "오늘의 "
            + mission.getTitle()
            + " 미션을 달성했어요!\n이번달 달성 미션 : "
            + monthlyMissionCount
            + "개",
        "/missions/today",
        "mission-completed:" + userId + ":" + missionId + ":" + completionDate);
    notifyTopThreeEntry(
        userId, missionId, completionDate, previousMonthlyRanking, currentMonthlyRanking);

    return MissionCompletionResponse.builder()
        .missionId(missionId)
        .missionType(mission.getMissionType())
        .completedAt(LocalDateTime.now(applicationClock))
        .safeMissionCount(badgeStatus.getSafeCount())
        .safeGrade(badgeStatus.getSafeGrade())
        .aggressiveMissionCount(badgeStatus.getAggressiveCount())
        .aggressiveGrade(badgeStatus.getAggressiveGrade())
        .build();
  }

  private void notifyTopThreeEntry(
      long userId,
      long missionId,
      LocalDate completionDate,
      ChallengeRankingMemberVo previousRanking,
      ChallengeRankingMemberVo currentRanking) {
    if (previousRanking == null
        || currentRanking == null
        || previousRanking.getRankingNo() <= 3
        || currentRanking.getRankingNo() > 3) {
      return;
    }
    notificationCommandService.createUserNotification(
        userId,
        NotificationType.RANKING_RISEN,
        "랭킹 상승",
        "이번달 동기 랭킹 TOP3 안에 들었어요!\n동기 랭킹 현황 보러 가기",
        "/challenges/ranking",
        "ranking-risen:"
            + userId
            + ":"
            + missionId
            + ":"
            + completionDate
            + ":"
            + currentRanking.getRankingNo());
  }

  private ChallengeRankingMemberVo findCurrentMonthlyRanking(
      long userId, ChallengeGroupVo group, LocalDate completionDate) {
    if (group == null) {
      return null;
    }
    return challengeGroupMapper.findMyRankingByGroupId(
        group.getGroupId(),
        userId,
        RankingPeriod.MONTHLY,
        completionDate.withDayOfMonth(1));
  }

  /** 오늘 노출할 지원 화면 연결 미션을 한 번의 조회로 가져옵니다. */
  private List<MissionVo> findTodayMissionVos(long userId) {
    return missionMapper.findTodayMissions(userId);
  }

  /** 성향별 완료 수와 티어를 갱신한 투자 뱃지 현황을 반환합니다. */
  private InvestmentBadgeStatusVo updateInvestmentBadge(long userId, MissionType missionType) {
    if (missionType == MissionType.SAFE) {
      missionMapper.incrementSafeMissionCount(userId);
    } else if (missionType == MissionType.AGGRESSIVE) {
      missionMapper.incrementAggressiveMissionCount(userId);
    } else {
      missionMapper.initializeInvestmentBadge(userId);
    }

    InvestmentBadgeStatusVo badgeStatus = missionMapper.findInvestmentBadgeStatus(userId);
    List<BadgeVo> activeBadges = missionMapper.findActiveBadges();
    syncEligibleUserBadges(userId, badgeStatus, activeBadges);
    updateGrade(userId, MissionType.SAFE, badgeStatus.getSafeCount(), activeBadges, badgeStatus);
    updateGrade(
        userId,
        MissionType.AGGRESSIVE,
        badgeStatus.getAggressiveCount(),
        activeBadges,
        badgeStatus);
    return badgeStatus;
  }

  /** 완료 수 기준에 해당하는 뱃지 획득 이력을 보정합니다. */
  private void syncEligibleUserBadges(
      long userId, InvestmentBadgeStatusVo badgeStatus, List<BadgeVo> activeBadges) {
    List<Long> eligibleBadgeIds =
        activeBadges.stream()
            .filter(badge -> isEligible(badge, badgeStatus))
            .map(BadgeVo::getBadgeId)
            .sorted()
            .toList();
    if (!eligibleBadgeIds.isEmpty()) {
      missionMapper.insertUserBadges(userId, eligibleBadgeIds);
    }
  }

  /** 뱃지 성향의 완료 수가 해당 뱃지 기준 이상인지 확인합니다. */
  private boolean isEligible(BadgeVo badge, InvestmentBadgeStatusVo badgeStatus) {
    int completionCount;
    if (badge.getMissionType() == MissionType.SAFE) {
      completionCount = badgeStatus.getSafeCount();
    } else if (badge.getMissionType() == MissionType.AGGRESSIVE) {
      completionCount = badgeStatus.getAggressiveCount();
    } else {
      return false;
    }
    return completionCount >= badge.getRequiredCompletionCount();
  }

  private void waitBeforeDeadlockRetry(int failedAttempt) {
    try {
      Thread.sleep(25L * failedAttempt);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ChallengeException(
          ChallengeErrorCode.MISSION_COMPLETION_RETRY_EXHAUSTED,
          "미션 완료 재시도 중 인터럽트가 발생했습니다.");
    }
  }

  /** 누적 완료 수에 해당하는 성향별 최고 티어를 저장합니다. */
  private void updateGrade(
      long userId,
      MissionType missionType,
      int completionCount,
      List<BadgeVo> activeBadges,
      InvestmentBadgeStatusVo badgeStatus) {
    String grade =
        activeBadges.stream()
            .filter(badge -> badge.getMissionType() == missionType)
            .filter(badge -> badge.getRequiredCompletionCount() <= completionCount)
            .max(Comparator.comparingInt(BadgeVo::getRequiredCompletionCount))
            .map(BadgeVo::getGrade)
            .orElse(null);
    String currentGrade =
        missionType == MissionType.SAFE
            ? badgeStatus.getSafeGrade()
            : badgeStatus.getAggressiveGrade();
    if (!Objects.equals(currentGrade, grade)) {
      missionMapper.updateInvestmentBadgeGrade(userId, missionType, grade);
      if (missionType == MissionType.SAFE) {
        badgeStatus.setSafeGrade(grade);
      } else {
        badgeStatus.setAggressiveGrade(grade);
      }
    }
  }
}
