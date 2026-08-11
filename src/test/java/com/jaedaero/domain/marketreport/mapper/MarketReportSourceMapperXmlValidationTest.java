package com.jaedaero.domain.marketreport.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class MarketReportSourceMapperXmlValidationTest {

  @Test
  void sourceMapperXmlIsParsedByMyBatis() throws Exception {
    Configuration configuration = new Configuration();
    try (InputStream inputStream =
        Resources.getResourceAsStream("mapper/marketreport/DailyMarketReportSourceMapper.xml")) {
      assertNotNull(inputStream);
      new XMLMapperBuilder(
              inputStream,
              configuration,
              "mapper/marketreport/DailyMarketReportSourceMapper.xml",
              configuration.getSqlFragments())
          .parse();
    }

    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper.insert"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper.findByReportId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.marketreport.mapper.DailyMarketReportSourceMapper.deleteByReportId"));
  }
}
