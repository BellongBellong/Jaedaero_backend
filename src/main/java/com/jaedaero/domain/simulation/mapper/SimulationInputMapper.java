package com.jaedaero.domain.simulation.mapper;

import com.jaedaero.domain.simulation.vo.SimulationInputSourceVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 임시 캐시플로우 입력 조회 Mapper.
 *
 * <p>캐시플로우 도메인 API가 제공되면 이 Mapper를 호출하는 Provider만 교체한다.
 */
@Mapper
public interface SimulationInputMapper {

  SimulationInputSourceVo findLatestByUserId(@Param("userId") long userId);
}
