package com.jaedaero.domain.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardSpendingMapper {

  Long sumThisMonthSpendingByUserId(@Param("userId") long userId);
}
