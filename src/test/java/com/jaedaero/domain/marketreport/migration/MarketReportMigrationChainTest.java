package com.jaedaero.domain.marketreport.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketReportMigrationChainTest {

  private static final Path MIGRATION_DIRECTORY = Path.of("sql", "migration");
  private static final String REPLACE_MIGRATION =
      "20260811_replace_ai_investment_report_with_gemini_market_report.sql";
  private static final String STRUCTURED_FIELDS_MIGRATION =
      "20260811_z_add_structured_market_report_fields.sql";

  @Test
  void titleAndSummaryAreAddedOnlyByTheLaterMigration() throws IOException {
    String replaceMigration = readMigration(REPLACE_MIGRATION);
    String structuredFieldsMigration = readMigration(STRUCTURED_FIELDS_MIGRATION);

    assertFalse(replaceMigration.contains("ADD COLUMN title"));
    assertFalse(replaceMigration.contains("ADD COLUMN summary"));
    assertFalse(replaceMigration.contains("MODIFY COLUMN title"));
    assertFalse(replaceMigration.contains("MODIFY COLUMN summary"));

    assertEquals(1, count(structuredFieldsMigration, "ADD COLUMN title"));
    assertEquals(1, count(structuredFieldsMigration, "ADD COLUMN summary"));
    assertEquals(1, count(structuredFieldsMigration, "MODIFY COLUMN title"));
    assertEquals(1, count(structuredFieldsMigration, "MODIFY COLUMN summary"));
    assertEquals(1, count(structuredFieldsMigration, "SET title ="));
    assertEquals(1, count(structuredFieldsMigration, "SET summary ="));
  }

  @Test
  void structuredFieldsMigrationSortsAfterReplaceMigrationAndInitialDdlContainsFields()
      throws IOException {
    List<String> migrationNames;
    try (var paths = Files.list(MIGRATION_DIRECTORY)) {
      migrationNames =
          paths.map(path -> path.getFileName().toString()).sorted().toList();
    }

    assertTrue(migrationNames.indexOf(REPLACE_MIGRATION) >= 0);
    assertTrue(migrationNames.indexOf(STRUCTURED_FIELDS_MIGRATION) >= 0);
    assertTrue(
        migrationNames.indexOf(REPLACE_MIGRATION)
            < migrationNames.indexOf(STRUCTURED_FIELDS_MIGRATION));

    String ddl = Files.readString(Path.of("jaedaero_db_v1.sql"));
    assertTrue(ddl.contains("title              VARCHAR(200) NOT NULL"));
    assertTrue(ddl.contains("summary            VARCHAR(500) NOT NULL"));
  }

  private String readMigration(String fileName) throws IOException {
    return Files.readString(MIGRATION_DIRECTORY.resolve(fileName));
  }

  private int count(String text, String fragment) {
    int count = 0;
    int offset = 0;
    while ((offset = text.indexOf(fragment, offset)) >= 0) {
      count++;
      offset += fragment.length();
    }
    return count;
  }
}
