package com.jaedaero.domain.challenge.service.impl;

import com.jaedaero.domain.challenge.common.enums.MissionCategory;
import com.jaedaero.domain.challenge.common.enums.MissionType;
import com.jaedaero.domain.challenge.dto.MissionCompletionResponse;
import com.jaedaero.domain.challenge.dto.MissionResponse;
import com.jaedaero.domain.challenge.exception.ChallengeErrorCode;
import com.jaedaero.domain.challenge.exception.ChallengeException;
import com.jaedaero.domain.challenge.mapper.MissionMapper;
import com.jaedaero.domain.challenge.service.MissionService;
import com.jaedaero.domain.challenge.vo.BadgeVo;
import com.jaedaero.domain.challenge.vo.InvestmentBadgeStatusVo;
import com.jaedaero.domain.challenge.vo.MissionVo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

  private final MissionMapper missionMapper;
  private final Clock applicationClock;

  /** 오늘 사용자에게 노출할 미션 목록을 조회합니다. */
  @Override
  @Transactional(readOnly = true)
  public List<MissionResponse> getTodayMissions(long userId) {
    return findTodayMissionVos(userId).stream().map(MissionResponse::from).toList();
  }

  /** 사용자의 미션 완료를 처리하고 뱃지 및 랭킹 집계를 갱신합니다. */
  @Override
  @Transactional
  public MissionCompletionResponse completeMission(long userId, long missionId) {
    MissionVo mission = missionMapper.findActiveMissionById(missionId);
    if (mission == null) {
      throw new ChallengeException(ChallengeErrorCode.MISSION_NOT_FOUND, "활성 미션을 찾을 수 없습니다.");
    }
    if (!isExposedToday(userId, missionId)) {
      throw new ChallengeException(ChallengeErrorCode.MISSION_NOT_AVAILABLE, "오늘 완료할 수 없는 미션입니다.");
    }

    validateNotCompleted(userId, mission);
    LocalDate completionDate = LocalDate.now(applicationClock);
    missionMapper.insertCompletion(userId, missionId, completionDate);

    InvestmentBadgeStatusVo badgeStatus = updateInvestmentBadge(userId, mission.getMissionType());
    missionMapper.incrementMonthlyChallengeMissionCount(userId);
    missionMapper.incrementTotalChallengeMissionCount(userId);

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

  /** 오늘 노출할 지원 화면 연결 미션을 조회합니다. */
  private List<MissionVo> findTodayMissionVos(long userId) {
    List<MissionVo> missions = new ArrayList<>(missionMapper.findDailyMissions(userId));
    addIfPresent(missions, missionMapper.findRecommendedMission(userId, MissionType.SAFE));
    addIfPresent(missions, missionMapper.findRecommendedMission(userId, MissionType.AGGRESSIVE));
    addIfPresent(missions, missionMapper.findOneTimeMission(userId));
    addIfPresent(missions, missionMapper.findEventMission(userId));
    return missions;
  }

  /** 조회 결과가 존재할 때만 미션 목록에 추가합니다. */
  private void addIfPresent(List<MissionVo> missions, MissionVo mission) {
    if (mission != null) {
      missions.add(mission);
    }
  }

  /** 요청한 미션이 오늘 사용자에게 노출된 미션인지 확인합니다. */
  private boolean isExposedToday(long userId, long missionId) {
    return findTodayMissionVos(userId).stream().anyMatch(mission -> mission.getMissionId() == missionId);
  }

  /** 미션 분류에 따라 당일 또는 전체 기간의 중복 완료를 검증합니다. */
  private void validateNotCompleted(long userId, MissionVo mission) {
    boolean completed =
        mission.getMissionCategory() == MissionCategory.ONE_TIME
            ? missionMapper.existsAnyCompletion(userId, mission.getMissionId())
            : missionMapper.existsCompletionToday(userId, mission.getMissionId());
    if (completed) {
      throw new ChallengeException(ChallengeErrorCode.MISSION_ALREADY_COMPLETED, "이미 완료한 미션입니다.");
    }
  }

  /** 성향별 완료 수와 티어를 갱신한 투자 뱃지 현황을 반환합니다. */
  private InvestmentBadgeStatusVo updateInvestmentBadge(long userId, MissionType missionType) {
    missionMapper.initializeInvestmentBadge(userId);
    if (missionType == MissionType.SAFE) {
      missionMapper.incrementSafeMissionCount(userId);
    } else if (missionType == MissionType.AGGRESSIVE) {
      missionMapper.incrementAggressiveMissionCount(userId);
    }

    InvestmentBadgeStatusVo badgeStatus = missionMapper.findInvestmentBadgeStatus(userId);
    List<BadgeVo> activeBadges = missionMapper.findActiveBadges();
    syncEligibleUserBadges(userId, badgeStatus, activeBadges);
    updateGrade(userId, MissionType.SAFE, badgeStatus.getSafeCount(), activeBadges);
    updateGrade(userId, MissionType.AGGRESSIVE, badgeStatus.getAggressiveCount(), activeBadges);
    return missionMapper.findInvestmentBadgeStatus(userId);
  }

  /** 완료 수 기준에 해당하는 뱃지 획득 이력을 보정합니다. */
  private void syncEligibleUserBadges(
      long userId, InvestmentBadgeStatusVo badgeStatus, List<BadgeVo> activeBadges) {
    activeBadges.stream()
        .filter(badge -> isEligible(badge, badgeStatus))
        .forEach(badge -> missionMapper.insertUserBadge(userId, badge.getBadgeId()));
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

  /** 누적 완료 수에 해당하는 성향별 최고 티어를 저장합니다. */
  private void updateGrade(
      long userId,
      MissionType missionType,
      int completionCount,
      List<BadgeVo> activeBadges) {
    String grade =
        activeBadges.stream()
            .filter(badge -> badge.getMissionType() == missionType)
            .filter(badge -> badge.getRequiredCompletionCount() <= completionCount)
            .max(Comparator.comparingInt(BadgeVo::getRequiredCompletionCount))
            .map(BadgeVo::getGrade)
            .orElse(null);
    missionMapper.updateInvestmentBadgeGrade(userId, missionType, grade);
  }
}
