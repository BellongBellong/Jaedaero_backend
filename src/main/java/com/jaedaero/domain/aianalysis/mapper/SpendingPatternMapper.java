package com.jaedaero.domain.aianalysis.mapper;

import com.jaedaero.domain.aianalysis.vo.RecurringPaymentAggregateVo;
import com.jaedaero.domain.aianalysis.vo.SpendingCategoryAggregateVo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SpendingPatternMapper {
  List<SpendingCategoryAggregateVo> aggregateByCategory(
      @Param("userId") long userId,
      @Param("currentStart") LocalDateTime currentStart,
      @Param("currentEndExclusive") LocalDateTime currentEndExclusive,
      @Param("previousStart") LocalDateTime previousStart,
      @Param("previousEndExclusive") LocalDateTime previousEndExclusive);

  List<RecurringPaymentAggregateVo> findRecurringPayments(
      @Param("userId") long userId,
      @Param("currentStart") LocalDateTime currentStart,
      @Param("currentEndExclusive") LocalDateTime currentEndExclusive,
      @Param("previousStart") LocalDateTime previousStart,
      @Param("previousEndExclusive") LocalDateTime previousEndExclusive);
}
