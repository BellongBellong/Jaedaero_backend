package com.jaedaero.domain.cashflow.mapper;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MilitaryPayPolicyMapper {

  Long findMonthlySalary(
      @Param("soldierType") String soldierType,
      @Param("rankName") String rankName,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd);
}
