package com.jaedaero.domain.dashboard.service;

import com.jaedaero.domain.dashboard.mapper.AssetSnapshotMapper;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 동기화된 계좌 잔액을 기준으로 사용자별 일일 자산 현황을 보관합니다. */
@Service
@RequiredArgsConstructor
public class AssetSnapshotService {

  private final AssetSnapshotMapper assetSnapshotMapper;
  private final Clock clock;

  @Transactional
  public int snapshotToday() {
    return assetSnapshotMapper.upsertDailyAssetSnapshots(LocalDate.now(clock));
  }
}
