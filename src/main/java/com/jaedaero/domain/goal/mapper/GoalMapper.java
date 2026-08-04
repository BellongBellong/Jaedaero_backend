package com.jaedaero.domain.goal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoalMapper {
  Long findTargetAmount(@Param("userId") long userId);
  int updateTargetAmount(@Param("userId") long userId, @Param("targetAmount") long targetAmount);
}
