package com.jaedaero.domain.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class AiStrategyMapperXmlValidationTest {

  private static final List<String> MAPPER_RESOURCES =
      List.of(
          "mapper/simulation/SimulationMapper.xml",
          "mapper/simulation/SimulationInputMapper.xml",
          "mapper/aianalysis/AiAnalysisMapper.xml",
          "mapper/aianalysis/AiAnalysisInputMapper.xml",
          "mapper/strategyapplication/StrategyApplicationMapper.xml",
          "mapper/rebalancing/RebalancingRecommendationMapper.xml",
          "mapper/marketreport/DailyMarketReportMapper.xml");

  @Test
  void aiStrategyMapperXmls_areParsedByMyBatis() throws Exception {
    Configuration configuration = new Configuration();

    for (String resource : MAPPER_RESOURCES) {
      try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
        assertNotNull(inputStream, () -> "Mapper XML을 찾을 수 없습니다: " + resource);
        new XMLMapperBuilder(
                inputStream, configuration, resource, configuration.getSqlFragments())
            .parse();
      }
    }

    assertTrue(
        configuration.hasStatement("com.jaedaero.domain.simulation.mapper.SimulationMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.simulation.mapper.SimulationInputMapper.findLatestByUserId"));
    assertTrue(
        configuration.hasStatement("com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper.insertAnalysis"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper.findLatestSuccessfulByUserIdAndInputDataHash"));
    assertTrue(configuration.hasStatement("com.jaedaero.domain.aianalysis.mapper.AiAnalysisInputMapper.findLatestByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.rebalancing.mapper.RebalancingRecommendationMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper.upsert"));
  }
}
