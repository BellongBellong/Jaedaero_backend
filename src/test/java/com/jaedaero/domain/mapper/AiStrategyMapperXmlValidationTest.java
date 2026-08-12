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
          "mapper/aianalysis/AiAnalysisMapper.xml",
          "mapper/aianalysis/AiAnalysisInputMapper.xml",
          "mapper/analysishistory/AnalysisHistoryMapper.xml",
          "mapper/strategyapplication/StrategyApplicationMapper.xml",
          "mapper/recurringinvestment/RecurringInvestmentPlanMapper.xml",
          "mapper/investmentguidance/InvestmentGuidanceInputMapper.xml",
          "mapper/investmentguidance/MockDbBrokeragePositionMapper.xml",
          "mapper/investmentguidance/InvestmentGuidanceMapper.xml",
          "mapper/marketreport/DailyMarketReportMapper.xml",
          "mapper/marketreport/DailyMarketIndicatorMapper.xml");

  @Test
  void aiStrategyMapperXmls_areParsedByMyBatis() throws Exception {
    Configuration configuration = new Configuration();

    for (String resource : MAPPER_RESOURCES) {
      try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
        assertNotNull(inputStream, () -> "Mapper XML을 찾을 수 없습니다: " + resource);
        new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments())
            .parse();
      }
    }

    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.simulation.mapper.SimulationMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper.insertAnalysis"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper.findLatestSuccessfulByUserIdAndInputDataHash"));
    assertTrue(configuration.hasStatement("com.jaedaero.domain.aianalysis.mapper.AiAnalysisInputMapper.findLatestByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.analysishistory.mapper.AnalysisHistoryMapper.findByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.analysishistory.mapper.AnalysisHistoryMapper.countByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.analysishistory.mapper.AnalysisHistoryMapper.findLatestByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper.findByAnalysisIdAndUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper.findLatestByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper.findByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceInputMapper.findLatestByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceInputMapper.findActiveTargetAmountByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.investmentguidance.mapper.MockDbBrokeragePositionMapper.findByUserIdAndAccountId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper.upsert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper.findLatest"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper.countByReportDate"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper.claimReportDate"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketIndicatorMapper.findByReportId"));
  }
}
