package com.jaedaero.domain.strategyapplication.mapper;

import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StrategyApplicationMapper {

  int insert(StrategyApplicationVo strategyApplication);

  List<StrategyApplicationVo> findByUserId(@Param("userId") long userId);
}
