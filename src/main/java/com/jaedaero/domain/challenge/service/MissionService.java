package com.jaedaero.domain.challenge.service;

import com.jaedaero.domain.challenge.dto.MissionCompletionResponse;
import com.jaedaero.domain.challenge.dto.MissionResponse;
import java.util.List;

public interface MissionService {

  /** 오늘 사용자에게 노출할 미션 목록을 조회합니다. */
  List<MissionResponse> getTodayMissions(long userId);

  /** 사용자의 미션 완료를 처리하고 뱃지 및 랭킹 집계를 갱신합니다. */
  MissionCompletionResponse completeMission(long userId, long missionId);
}
