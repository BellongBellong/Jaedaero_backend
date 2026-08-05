package com.jaedaero.domain.goal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoalMapper {
  /** 활성 사용자의 목표 금액을 조회합니다. */
  Long findTargetAmount(@Param("userId") long userId);

  /** 활성 사용자의 목표 금액을 변경합니다. */
  int updateTargetAmount(@Param("userId") long userId, @Param("targetAmount") long targetAmount);
}
