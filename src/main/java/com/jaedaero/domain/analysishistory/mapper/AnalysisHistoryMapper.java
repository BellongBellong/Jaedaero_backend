package com.jaedaero.domain.analysishistory.mapper;

import com.jaedaero.domain.analysishistory.vo.AnalysisHistoryRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnalysisHistoryMapper {

  List<AnalysisHistoryRow> findByUserId(
      @Param("userId") long userId,
      @Param("type") String type,
      @Param("offset") long offset,
      @Param("limit") int limit);

  long countByUserId(@Param("userId") long userId, @Param("type") String type);

  AnalysisHistoryRow findLatestByUserId(
      @Param("userId") long userId, @Param("type") String type);
}
