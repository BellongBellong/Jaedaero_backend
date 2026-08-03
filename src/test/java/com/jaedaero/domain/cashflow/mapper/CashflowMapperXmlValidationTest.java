package com.jaedaero.domain.cashflow.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class CashflowMapperXmlValidationTest {

  @Test
  void cashflowMapperXml_isParsedByMyBatis() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "mapper/cashflow/CashflowMapper.xml";

    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      assertNotNull(inputStream);
      new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.cashflow.mapper.CashflowMapper.findInputByUserId"));
    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.cashflow.mapper.CashflowMapper.insertForecastMonths"));
  }

  @Test
  void militaryPayPolicyMapperXml_isParsedByMyBatis() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "mapper/cashflow/MilitaryPayPolicyMapper.xml";

    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      assertNotNull(inputStream);
      new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
    }

    assertTrue(
        configuration.hasStatement(
            "com.jaedaero.domain.cashflow.mapper.MilitaryPayPolicyMapper.findMonthlySalary"));
  }
}
