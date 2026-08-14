package com.jaedaero.domain.dashboard.mapper;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetSnapshotMapper {

  int upsertDailyAssetSnapshots(@Param("snapshotDate") LocalDate snapshotDate);
}
