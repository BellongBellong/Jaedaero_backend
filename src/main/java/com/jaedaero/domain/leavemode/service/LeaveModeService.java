package com.jaedaero.domain.leavemode.service;

import com.jaedaero.domain.leavemode.dto.LeaveModeRequest;
import com.jaedaero.domain.leavemode.dto.LeaveModeBudgetRequest;
import com.jaedaero.domain.leavemode.dto.LeaveModeResponse;

public interface LeaveModeService {

  /** 사용자의 휴가모드 일정을 등록합니다. */
  LeaveModeResponse startLeaveMode(long userId, LeaveModeRequest request);

  /** 사용자의 현재 휴가모드 상태를 조회합니다. */
  LeaveModeResponse getCurrentLeaveMode(long userId);

  /** 사용자의 휴가 예산을 변경합니다. */
  LeaveModeResponse updateBudget(long userId, long leaveModeId, LeaveModeBudgetRequest request);

  /** 사용자의 휴가모드 일정을 비활성화합니다. */
  void deleteLeaveMode(long userId, long leaveModeId);
}
