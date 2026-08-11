package com.jaedaero.domain.marketreport.mapper;

import com.jaedaero.domain.marketreport.vo.DailyMarketIndicatorVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyMarketIndicatorMapper {
  int insert(DailyMarketIndicatorVo indicator);

  int deleteByReportId(@Param("reportId") long reportId);

  List<DailyMarketIndicatorVo> findByReportId(@Param("reportId") long reportId);
}
