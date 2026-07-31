package com.jaedaero.domain.simulation.mapper;

import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SimulationMapper {

  int insert(SimulationVo simulation);

  SimulationVo findByIdAndUserId(
      @Param("simulationId") long simulationId, @Param("userId") long userId);

  List<SimulationVo> findByUserId(
      @Param("userId") long userId, @Param("offset") int offset, @Param("limit") int limit);

  long countByUserId(@Param("userId") long userId);
}
