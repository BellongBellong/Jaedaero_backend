package com.jaedaero.domain.recurringinvestment.mapper;

import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecurringInvestmentPlanMapper {

  int insert(RecurringInvestmentPlanVo plan);

  int update(RecurringInvestmentPlanVo plan);

  RecurringInvestmentPlanVo findByUserId(@Param("userId") long userId);

  RecurringInvestmentPlanVo findByIdAndUserId(
      @Param("planId") long planId, @Param("userId") long userId);

  boolean existsSecuritiesAccount(
      @Param("accountId") long accountId, @Param("userId") long userId);
}
