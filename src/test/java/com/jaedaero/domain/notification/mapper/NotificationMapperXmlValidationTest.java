package com.jaedaero.domain.notification.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class NotificationMapperXmlValidationTest {

  @Test
  void notificationMappersAreParsedAndRegisterCriticalStatements() throws Exception {
    Configuration configuration = new Configuration();
    parse(configuration, "mapper/notification/DeviceTokenMapper.xml");
    parse(configuration, "mapper/notification/NotificationMapper.xml");
    parse(configuration, "mapper/notification/NotificationOutboxMapper.xml");

    List<String> statements =
        List.of(
            DeviceTokenMapper.class.getName() + ".upsert",
            NotificationMapper.class.getName() + ".findFeed",
            NotificationMapper.class.getName() + ".insertCampaignReceipt",
            NotificationMapper.class.getName() + ".findDailyMissionTargetUserIds",
            NotificationOutboxMapper.class.getName() + ".findClaimableForUpdate",
            NotificationOutboxMapper.class.getName() + ".markRetry");
    statements.forEach(statement -> assertTrue(configuration.hasStatement(statement), statement));
  }

  private void parse(Configuration configuration, String resource) throws Exception {
    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      assertNotNull(inputStream);
      new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments())
          .parse();
    }
  }
}
