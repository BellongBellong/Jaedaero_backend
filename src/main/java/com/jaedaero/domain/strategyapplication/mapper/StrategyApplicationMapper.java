package com.jaedaero.domain.strategyapplication.mapper;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StrategyApplicationMapper {

  int insert(StrategyApplicationVo strategyApplication);

  int updateAfterExpectedAsset(
      @Param("applicationId") long applicationId,
      @Param("userId") long userId,
      @Param("afterExpectedAsset") long afterExpectedAsset);

  StrategyApplicationVo findByIdAndUserId(
      @Param("applicationId") long applicationId, @Param("userId") long userId);

  StrategyApplicationVo findLatestByUserId(@Param("userId") long userId);

  StrategyApplicationVo findByGuidanceSelection(
      @Param("userId") long userId,
      @Param("guidanceId") long guidanceId,
      @Param("action") InvestmentGuidanceAction action,
      @Param("frequency") InvestmentFrequency frequency,
      @Param("contributionAmount") long contributionAmount);

  List<StrategyApplicationVo> findByUserId(
      @Param("userId") long userId,
      @Param("offset") long offset,
      @Param("limit") int limit);
}
