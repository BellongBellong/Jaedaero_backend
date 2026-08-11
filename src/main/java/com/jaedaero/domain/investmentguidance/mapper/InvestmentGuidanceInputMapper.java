package com.jaedaero.domain.investmentguidance.mapper;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceInputSourceVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvestmentGuidanceInputMapper {

  Long findActiveTargetAmountByUserId(@Param("userId") long userId);

  InvestmentGuidanceInputSourceVo findLatestByUserId(
      @Param("userId") long userId, @Param("brokerageAccountId") long brokerageAccountId);

  default String findCurrentRankNameByUserId(@Param("userId") long userId) {
    return null;
  }
}
