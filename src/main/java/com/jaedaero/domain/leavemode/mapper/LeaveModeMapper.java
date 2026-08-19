package com.jaedaero.domain.leavemode.mapper;

import com.jaedaero.domain.leavemode.vo.LeaveModeVo;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LeaveModeMapper {

  /** 휴가모드 일정을 저장합니다. */
  int insert(LeaveModeVo leaveMode);

  /** 기준일에 활성화된 휴가모드 일정을 조회합니다. */
  LeaveModeVo findCurrentByUserId(
      @Param("userId") long userId, @Param("referenceDate") LocalDate referenceDate);

  /** 사용자의 삭제되지 않은 이벤트 목록을 조회합니다. */
  List<LeaveModeVo> findAllByUserId(@Param("userId") long userId);

  /** 사용자의 휴가 예산을 변경합니다. */
  int updateBudgetAmount(
      @Param("userId") long userId,
      @Param("leaveModeId") long leaveModeId,
      @Param("budgetAmount") Long budgetAmount);

  /** 사용자의 이벤트를 삭제 처리합니다. */
  int deleteByUserId(@Param("userId") long userId, @Param("leaveModeId") long leaveModeId);
}
