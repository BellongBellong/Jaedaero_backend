package com.jaedaero.domain.codef.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CodefPersistenceRepositoryTransactionCategoryIntegrationTest {

  @Test
  void selectsOnlyRuleEtcWithdrawalsAndDoesNotOverwriteUserCorrection() {
    JdbcTemplate jdbcTemplate =
        new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:transaction_category_"
                    + System.nanoTime()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""));
    createSchema(jdbcTemplate);
    insert(jdbcTemplate, 1L, "WITHDRAW", "ETC", "RULE", "분류 대상");
    insert(jdbcTemplate, 2L, "WITHDRAW", "FOOD", "RULE", "기존 규칙 분류");
    insert(jdbcTemplate, 3L, "DEPOSIT", "ETC", "RULE", "입금");
    insert(jdbcTemplate, 4L, "WITHDRAW", "ETC", "USER", "사용자 수정");
    CodefPersistenceRepository repository = new CodefPersistenceRepository(jdbcTemplate);

    assertEquals(
        List.of(1L),
        repository.findRuleUnclassifiedWithdrawals(10).stream()
            .map(StoredTransactionCategoryCandidate::transactionId)
            .toList());
    assertEquals(1, repository.updateTransactionCategoryFromAiIfUnclassified(1L, "FOOD"));
    assertEquals(0, repository.updateTransactionCategoryFromAiIfUnclassified(4L, "FOOD"));
    assertEquals(
        "FOOD:AI",
        jdbcTemplate.queryForObject(
            "SELECT CONCAT(category, ':', category_source) FROM transaction_history WHERE transaction_id = 1",
            String.class));
    assertEquals(
        "ETC:USER",
        jdbcTemplate.queryForObject(
            "SELECT CONCAT(category, ':', category_source) FROM transaction_history WHERE transaction_id = 4",
            String.class));
  }

  private void createSchema(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute(
        "CREATE TABLE transaction_history (transaction_id BIGINT PRIMARY KEY, account_id BIGINT, "
            + "transaction_datetime TIMESTAMP NOT NULL, transaction_type VARCHAR(20), "
            + "category VARCHAR(50), category_source VARCHAR(20), "
            + "transaction_description VARCHAR(255))");
  }

  private void insert(
      JdbcTemplate jdbcTemplate,
      long id,
      String type,
      String category,
      String source,
      String description) {
    jdbcTemplate.update(
        "INSERT INTO transaction_history VALUES (?, 10, CURRENT_TIMESTAMP, ?, ?, ?, ?)",
        id,
        type,
        category,
        source,
        description);
  }
}
