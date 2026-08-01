package com.jaedaero.domain.cashflow.mapper;

import com.jaedaero.domain.cashflow.vo.CashflowForecastMonthVo;
import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import com.jaedaero.domain.cashflow.vo.CashflowInputSourceVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CashflowMapper {

  CashflowInputSourceVo findInputByUserId(@Param("userId") long userId);

  void insertForecast(CashflowForecastVo forecast);

  void insertForecastMonths(
      @Param("forecastId") long forecastId, @Param("months") List<CashflowForecastMonthVo> months);

  CashflowForecastVo findLatestForecastByUserId(@Param("userId") long userId);

  List<CashflowForecastMonthVo> findMonthsByForecastId(@Param("forecastId") long forecastId);
}
