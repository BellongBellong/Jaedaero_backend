package com.jaedaero.domain.marketreport.mapper;

import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyMarketReportMapper {

  int upsert(DailyMarketReportVo report);

  DailyMarketReportVo findActiveAt(@Param("now") LocalDateTime now);

  DailyMarketReportVo findLatest();

  int countByReportDate(@Param("reportDate") LocalDate reportDate);

  /**
   * report_date UNIQUE 제약을 이용해 오늘자 배치 실행권을 원자적으로 선점한다. 이미 다른 실행(스케줄러·수동
   * 트리거)이 선점했으면 0을 반환한다(INSERT IGNORE) — 일반 배치는 그 경우 이후 지표 수집·Gemini 호출을
   * 건너뛴다. 로컬 재시험은 별도의 row-lock claim을 사용한다.
   */
  int claimReportDate(
      @Param("reportDate") LocalDate reportDate,
      @Param("validFrom") LocalDateTime validFrom,
      @Param("validUntil") LocalDateTime validUntil);

  /** 날짜 행을 잠근 상태로 읽어 로컬 재시험 claim의 동시성을 제어한다. */
  DailyMarketReportVo findByReportDateForUpdate(@Param("reportDate") LocalDate reportDate);

  /** 성공 Gemini 리포트가 아닌 행을 생성 진행 상태로 원자적으로 표시한다. */
  int markGenerationInProgress(@Param("reportId") long reportId);
}
