package com.jaedaero.domain.rebalancing.mapper;

import com.jaedaero.domain.rebalancing.vo.RebalancingRecommendationVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RebalancingRecommendationMapper {

  int insert(RebalancingRecommendationVo recommendation);

  RebalancingRecommendationVo findLatestByUserId(@Param("userId") long userId);

  RebalancingRecommendationVo findByIdAndUserId(
      @Param("rebalancingId") long rebalancingId, @Param("userId") long userId);
}
