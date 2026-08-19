package com.jaedaero.domain.leavemode.service.impl;

import com.jaedaero.domain.leavemode.dto.LeaveModeRequest;
import com.jaedaero.domain.leavemode.dto.LeaveModeBudgetRequest;
import com.jaedaero.domain.leavemode.dto.LeaveModeResponse;
import com.jaedaero.domain.leavemode.exception.LeaveModeErrorCode;
import com.jaedaero.domain.leavemode.exception.LeaveModeException;
import com.jaedaero.domain.leavemode.mapper.LeaveModeMapper;
import com.jaedaero.domain.leavemode.service.LeaveModeService;
import com.jaedaero.domain.leavemode.vo.LeaveModeVo;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveModeServiceImpl implements LeaveModeService {

  private final LeaveModeMapper leaveModeMapper;
  private final Clock applicationClock;

  /** 사용자의 휴가모드 일정을 등록합니다. */
  @Override
  @Transactional
  public LeaveModeResponse startLeaveMode(long userId, LeaveModeRequest request) {
    LeaveModeVo leaveMode = new LeaveModeVo();
    leaveMode.setUserId(userId);
    leaveMode.setEventName(request.getEventName());
    leaveMode.setStartDate(request.getStartDate());
    leaveMode.setEndDate(request.getEndDate());
    leaveMode.setLeaveModeEnabled(request.isLeaveModeEnabled());
    leaveMode.setBudgetAmount(request.getBudgetAmount());
    leaveModeMapper.insert(leaveMode);
    return toResponse(leaveMode);
  }

  /** 사용자의 현재 휴가모드 상태를 조회합니다. */
  @Override
  @Transactional(readOnly = true)
  public LeaveModeResponse getCurrentLeaveMode(long userId) {
    LeaveModeVo leaveMode =
        leaveModeMapper.findCurrentByUserId(userId, LocalDate.now(applicationClock));
    return leaveMode == null ? null : toResponse(leaveMode);
  }

  /** 사용자의 휴가 예산을 변경합니다. */
  @Override
  @Transactional
  public LeaveModeResponse updateBudget(
      long userId, long leaveModeId, LeaveModeBudgetRequest request) {
    LeaveModeVo leaveMode = leaveModeMapper.findCurrentByUserId(userId, LocalDate.now(applicationClock));
    if (leaveMode == null || leaveMode.getLeaveModeId() != leaveModeId) {
      throw new LeaveModeException(LeaveModeErrorCode.LEAVE_MODE_NOT_FOUND, "활성 휴가 일정을 찾을 수 없습니다.");
    }
    leaveModeMapper.updateBudgetAmount(userId, leaveModeId, request.getBudgetAmount());
    leaveMode.setBudgetAmount(request.getBudgetAmount());
    return toResponse(leaveMode);
  }

  /** 사용자의 휴가모드 일정을 비활성화합니다. */
  @Override
  @Transactional
  public void deleteLeaveMode(long userId, long leaveModeId) {
    if (leaveModeMapper.deactivateByUserId(userId, leaveModeId) == 0) {
      throw new LeaveModeException(LeaveModeErrorCode.LEAVE_MODE_NOT_FOUND, "활성 휴가 일정을 찾을 수 없습니다.");
    }
  }

  private LeaveModeResponse toResponse(LeaveModeVo leaveMode) {
    return new LeaveModeResponse(
        leaveMode.getLeaveModeId(),
        leaveMode.getEventName(),
        leaveMode.getStartDate(),
        leaveMode.getEndDate(),
        leaveMode.isLeaveModeEnabled(),
        leaveMode.getBudgetAmount());
  }
}
