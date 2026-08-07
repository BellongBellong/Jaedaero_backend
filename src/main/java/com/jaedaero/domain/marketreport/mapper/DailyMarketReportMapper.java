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
}
