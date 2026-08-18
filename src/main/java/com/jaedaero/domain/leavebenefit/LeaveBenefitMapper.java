package com.jaedaero.domain.leavebenefit;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LeaveBenefitMapper {

  List<LeaveBenefitResponse> findAvailableBenefits(
      @Param("category") LeaveBenefitCategory category,
      @Param("referenceDate") LocalDate referenceDate);
}
