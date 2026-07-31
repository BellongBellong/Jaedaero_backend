package com.jaedaero.domain.auth.mapper;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvestmentPreferenceMapper {

  /** 활성 사용자 존재 여부를 조회합니다. */
  int countActiveUserByUserId(@Param("userId") long userId);

  /** 사용자의 초기 투자 성향을 등록하거나 갱신합니다. */
  void upsertInitialPreference(
      @Param("userId") long userId,
      @Param("investmentPreference") InvestmentPreference investmentPreference);

  /** 사용자의 목표 금액을 등록하거나 갱신합니다. */
  void upsertGoalTargetAmount(@Param("userId") long userId, @Param("targetAmount") long targetAmount);
}
