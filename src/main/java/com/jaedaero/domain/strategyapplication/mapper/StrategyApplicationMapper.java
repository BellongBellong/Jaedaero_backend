package com.jaedaero.domain.strategyapplication.mapper;

import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StrategyApplicationMapper {

  int insert(StrategyApplicationVo strategyApplication);

  StrategyApplicationVo findByIdAndUserId(
      @Param("applicationId") long applicationId, @Param("userId") long userId);

  StrategyApplicationVo findLatestByUserId(@Param("userId") long userId);

  List<StrategyApplicationVo> findByUserId(
      @Param("userId") long userId,
      @Param("offset") long offset,
      @Param("limit") int limit);
}
