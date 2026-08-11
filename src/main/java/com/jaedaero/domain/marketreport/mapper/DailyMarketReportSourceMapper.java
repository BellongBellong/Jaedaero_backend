package com.jaedaero.domain.marketreport.mapper;

import com.jaedaero.domain.marketreport.vo.DailyMarketReportSourceVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyMarketReportSourceMapper {
  int insert(DailyMarketReportSourceVo source);

  int deleteByReportId(@Param("reportId") long reportId);

  List<DailyMarketReportSourceVo> findByReportId(@Param("reportId") long reportId);
}
