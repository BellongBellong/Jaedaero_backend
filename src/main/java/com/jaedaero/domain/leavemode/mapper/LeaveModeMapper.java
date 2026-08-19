package com.jaedaero.domain.leavemode.mapper;

import com.jaedaero.domain.leavemode.vo.LeaveModeVo;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LeaveModeMapper {

  /** 휴가모드 일정을 저장합니다. */
  int insert(LeaveModeVo leaveMode);

  /** 기준일에 활성화된 휴가모드 일정을 조회합니다. */
  LeaveModeVo findCurrentByUserId(
      @Param("userId") long userId, @Param("referenceDate") LocalDate referenceDate);

  /** 사용자의 휴가 예산을 변경합니다. */
  int updateBudgetAmount(
      @Param("userId") long userId,
      @Param("leaveModeId") long leaveModeId,
      @Param("budgetAmount") Long budgetAmount);

  /** 사용자의 휴가모드 일정을 비활성화합니다. */
  int deactivateByUserId(@Param("userId") long userId, @Param("leaveModeId") long leaveModeId);
}
