package com.jaedaero.domain.auth.service.impl;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.auth.dto.MilitaryInfoRequest;
import com.jaedaero.domain.auth.dto.SoldierProfileResponse;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.MilitaryInfoException;
import com.jaedaero.domain.auth.mapper.MilitaryInfoMapper;
import com.jaedaero.domain.auth.service.MilitaryInfoService;
import com.jaedaero.domain.auth.vo.SoldierProfileVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MilitaryInfoServiceImpl implements MilitaryInfoService {

  private final MilitaryInfoMapper militaryInfoMapper;

  /** 사용자의 군 복무 정보를 저장하고 챌린지 그룹에 등록합니다. */
  @Override
  @Transactional
  public SoldierProfileResponse registerMilitaryInfo(long userId, MilitaryInfoRequest request) {
    validateRequest(request);
    if (militaryInfoMapper.countActiveUserByUserId(userId) == 0) {
      throw new MilitaryInfoException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }

    LocalDate dischargeDate = calculateDischargeDate(request.getSoldierType(), request.getEnlistmentDate());
    boolean savingJoinYn = militaryInfoMapper.countSoldierSavingByUserId(userId) > 0;
    SoldierProfileVo soldierProfile = new SoldierProfileVo();
    soldierProfile.setUserId(userId);
    soldierProfile.setSoldierType(request.getSoldierType());
    soldierProfile.setRankName(request.getRankName().trim());
    soldierProfile.setEnlistmentDate(request.getEnlistmentDate());
    soldierProfile.setDischargeDate(dischargeDate);
    soldierProfile.setSavingJoinYn(savingJoinYn);
    militaryInfoMapper.upsertSoldierProfile(soldierProfile);
    long challengeGroupId =
        registerChallengeMember(userId, soldierProfile.getSoldierType(), soldierProfile.getEnlistmentDate());
    BigDecimal challengeGroupTargetAmountAverage =
        militaryInfoMapper.findChallengeGroupTargetAmountAverage(challengeGroupId);

    return new SoldierProfileResponse(
        true,
        soldierProfile.getSoldierType(),
        soldierProfile.getRankName(),
        soldierProfile.getEnlistmentDate(),
        soldierProfile.getDischargeDate(),
        soldierProfile.isSavingJoinYn(),
        challengeGroupTargetAmountAverage);
  }

  /** 군종별 복무 기간을 기준으로 전역일을 계산합니다. */
  private LocalDate calculateDischargeDate(SoldierType soldierType, LocalDate enlistmentDate) {
    if (soldierType == null || enlistmentDate == null) {
      throw new MilitaryInfoException(AuthErrorCode.INVALID_MILITARY_INFO, "군종과 입대일은 필수입니다.");
    }

    switch (soldierType) {
      case ARMY:
      case MARINE:
        return enlistmentDate.plusMonths(18);
      case NAVY:
        return enlistmentDate.plusMonths(20);
      case AIRFORCE:
        return enlistmentDate.plusMonths(21);
      default:
        throw new MilitaryInfoException(AuthErrorCode.INVALID_MILITARY_INFO, "지원하지 않는 군종입니다.");
    }
  }

  /** 군 복무 정보 입력값을 검증합니다. */
  private void validateRequest(MilitaryInfoRequest request) {
    if (request == null
        || request.getRankName() == null
        || request.getRankName().trim().isEmpty()
        || request.getRankName().trim().length() > 20) {
      throw new MilitaryInfoException(AuthErrorCode.INVALID_MILITARY_INFO, "현재 계급이 올바르지 않습니다.");
    }
  }

  /** 입대 월과 군종 기준 챌린지 그룹에 사용자를 등록합니다. */
  private long registerChallengeMember(
      long userId, SoldierType soldierType, LocalDate enlistmentDate) {
    int enlistmentYear = enlistmentDate.getYear();
    int enlistmentMonth = enlistmentDate.getMonthValue();
    militaryInfoMapper.insertChallengeGroupIgnore(
        soldierType.name(), enlistmentYear, enlistmentMonth);
    Long groupId =
        militaryInfoMapper.findChallengeGroupId(soldierType.name(), enlistmentYear, enlistmentMonth);
    if (groupId == null) {
      throw new MilitaryInfoException(
          AuthErrorCode.INVALID_MILITARY_INFO, "입대 동기 챌린지 그룹을 생성할 수 없습니다.");
    }
    militaryInfoMapper.insertChallengeMemberIgnore(groupId, userId);
    return groupId;
  }
}
