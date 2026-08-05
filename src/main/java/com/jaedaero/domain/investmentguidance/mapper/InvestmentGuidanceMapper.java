package com.jaedaero.domain.investmentguidance.mapper;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceVo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvestmentGuidanceMapper {

  int insert(InvestmentGuidanceVo guidance);

  InvestmentGuidanceVo findByIdAndUserId(
      @Param("guidanceId") long guidanceId, @Param("userId") long userId);

  InvestmentGuidanceVo findLatestByUserId(
      @Param("userId") long userId, @Param("now") LocalDateTime now);

  InvestmentGuidanceVo findLatestByUserIdAndInputDataHash(
      @Param("userId") long userId, @Param("inputDataHash") String inputDataHash);
}
