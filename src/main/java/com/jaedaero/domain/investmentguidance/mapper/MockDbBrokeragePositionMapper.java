package com.jaedaero.domain.investmentguidance.mapper;

import com.jaedaero.domain.investmentguidance.vo.MockDbBrokeragePositionSourceVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MockDbBrokeragePositionMapper {

  MockDbBrokeragePositionSourceVo findByUserIdAndAccountId(
      @Param("userId") long userId, @Param("accountId") long accountId);
}
