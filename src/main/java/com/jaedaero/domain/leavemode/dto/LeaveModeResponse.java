package com.jaedaero.domain.leavemode.dto;

import java.time.LocalDate;

public record LeaveModeResponse(
    long leaveModeId,
    String eventName,
    LocalDate startDate,
    LocalDate endDate,
    boolean isLeaveModeEnabled,
    Long budgetAmount) {}
