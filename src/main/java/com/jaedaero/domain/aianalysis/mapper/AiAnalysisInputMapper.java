package com.jaedaero.domain.aianalysis.mapper;

import com.jaedaero.domain.aianalysis.vo.AiAnalysisInputSourceVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAnalysisInputMapper {
  AiAnalysisInputSourceVo findLatestByUserId(@Param("userId") long userId);
}
