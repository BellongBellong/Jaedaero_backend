package com.jaedaero.domain.codef.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CodefPersistenceRepositoryTimezoneIntegrationTest {

  @Test
  void keepsDateBoundariesWhenJvmAndDatabaseSessionTimezonesDiffer() {
    TimeZone originalTimezone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    try {
      JdbcTemplate jdbcTemplate =
          new JdbcTemplate(
              new DriverManagerDataSource(
                  "jdbc:h2:mem:transaction_timezone_"
                      + System.nanoTime()
                      + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                  "sa",
                  ""));
      jdbcTemplate.execute("SET TIME ZONE 'Asia/Seoul'");
      createSchema(jdbcTemplate);
      insertTransactions(jdbcTemplate);
      CodefPersistenceRepository repository = new CodefPersistenceRepository(jdbcTemplate);

      LocalDate date = LocalDate.of(2026, 8, 12);
      assertEquals(
          List.of(2L, 1L),
          repository.findTransactions(10L, date, date).stream()
              .map(StoredTransaction::transactionId)
              .toList());
      assertEquals(
          List.of(2L, 1L),
          repository.findTransactionsByUser(7L, null, date, date, null).stream()
              .map(StoredTransaction::transactionId)
              .toList());
    } finally {
      TimeZone.setDefault(originalTimezone);
    }
  }

  private void createSchema(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute(
        "CREATE TABLE codef_connection (connection_id BIGINT PRIMARY KEY, user_id BIGINT, status VARCHAR(20))");
    jdbcTemplate.execute(
        "CREATE TABLE connected_account (account_id BIGINT PRIMARY KEY, connection_id BIGINT, status VARCHAR(20))");
    jdbcTemplate.execute(
        "CREATE TABLE transaction_history (transaction_id BIGINT PRIMARY KEY, account_id BIGINT, "
            + "transaction_datetime TIMESTAMP NOT NULL, amount BIGINT, balance_after BIGINT, "
            + "transaction_type VARCHAR(20), category VARCHAR(50), transaction_description VARCHAR(255))");
    jdbcTemplate.update("INSERT INTO codef_connection VALUES (?, ?, ?)", 1L, 7L, "ACTIVE");
    jdbcTemplate.update("INSERT INTO connected_account VALUES (?, ?, ?)", 10L, 1L, "ACTIVE");
  }

  private void insertTransactions(JdbcTemplate jdbcTemplate) {
    insert(jdbcTemplate, 1L, LocalDateTime.of(2026, 8, 12, 0, 0));
    insert(jdbcTemplate, 2L, LocalDateTime.of(2026, 8, 12, 23, 59, 59));
    insert(jdbcTemplate, 3L, LocalDateTime.of(2026, 8, 13, 0, 0));
  }

  private void insert(JdbcTemplate jdbcTemplate, long transactionId, LocalDateTime transactionAt) {
    jdbcTemplate.update(
        "INSERT INTO transaction_history VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        transactionId,
        10L,
        transactionAt,
        1000L,
        1000L,
        "DEPOSIT",
        null,
        "테스트");
  }
}
