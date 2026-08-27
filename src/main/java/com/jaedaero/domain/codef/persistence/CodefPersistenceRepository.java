package com.jaedaero.domain.codef.persistence;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CodefPersistenceRepository {

  private final JdbcTemplate jdbcTemplate;

  public CodefPersistenceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean existsUser(long userId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE user_id = ?", Integer.class, userId);
    return count != null && count > 0;
  }

  public Optional<StoredCodefConnection> findConnectionByUserId(long userId) {
    List<StoredCodefConnection> rows =
        jdbcTemplate.query(
            "SELECT connection_id, user_id, connected_id_encrypted FROM codef_connection "
                + "WHERE user_id = ? AND status = 'ACTIVE'",
            (rs, rowNum) ->
                new StoredCodefConnection(
                    rs.getLong("connection_id"),
                    rs.getLong("user_id"),
                    rs.getString("connected_id_encrypted")),
            userId);
    return rows.stream().findFirst();
  }

  /** 시연용 계좌가 사용자 입력을 통해 활성화되었는지 확인합니다. */
  public boolean isDemoConnectionActivated(long userId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM codef_connection WHERE user_id = ? "
                + "AND last_sync_error_message = 'DEMO_CONNECTION'",
            Integer.class,
            userId);
    return count != null && count > 0;
  }

  /** 매일 일괄 동기화할 활성 CODEF 연결 사용자를 반환합니다. */
  public List<Long> findActiveConnectionUserIds() {
    return jdbcTemplate.query(
        "SELECT DISTINCT user_id FROM codef_connection WHERE status = 'ACTIVE' ORDER BY user_id",
        (rs, rowNum) -> rs.getLong("user_id"));
  }

  public void saveConnection(long userId, String encryptedConnectedId, String connectedIdHash) {
    jdbcTemplate.update(
        "INSERT INTO codef_connection (user_id, connected_id_encrypted, connected_id_hash, status) "
            + "VALUES (?, ?, ?, 'ACTIVE') "
            + "ON DUPLICATE KEY UPDATE connected_id_encrypted = VALUES(connected_id_encrypted), "
            + "connected_id_hash = VALUES(connected_id_hash), status = 'ACTIVE', "
            + "last_sync_error_message = NULL",
        userId,
        encryptedConnectedId,
        connectedIdHash);
  }

  public void updateConnectionSyncSuccess(long connectionId) {
    jdbcTemplate.update(
        "UPDATE codef_connection SET last_sync_at = CURRENT_TIMESTAMP, last_sync_error_message ="
            + " NULL WHERE connection_id = ?",
        connectionId);
  }

  public void updateConnectionSyncError(long connectionId, String message) {
    jdbcTemplate.update(
        "UPDATE codef_connection SET last_sync_error_message = ? WHERE connection_id = ?",
        truncate(message, 500),
        connectionId);
  }

  /** 시연 전용 연결은 CODEF 일괄 동기화 대상에서 제외합니다. */
  public void markConnectionAsDemo(long connectionId) {
    jdbcTemplate.update(
        "UPDATE codef_connection SET status = 'ERROR', last_sync_error_message = 'DEMO_CONNECTION' "
            + "WHERE connection_id = ?",
        connectionId);
  }

  public Optional<StoredInstitutionConnection> findInstitutionConnection(
      long userId, String institutionCode, String businessType) {
    List<StoredInstitutionConnection> rows =
        jdbcTemplate.query(
            "SELECT cic.institution_connection_id, cic.connection_id, cic.institution_code,"
                + " cic.business_type, cic.status, cic.login_id_encrypted,"
                + " cic.login_password_encrypted, cic.birth_date_encrypted FROM"
                + " codef_institution_connection cic JOIN codef_connection cc ON cc.connection_id ="
                + " cic.connection_id WHERE cc.user_id = ? AND cic.institution_code = ? AND"
                + " cic.business_type = ?",
            (rs, rowNum) ->
                new StoredInstitutionConnection(
                    rs.getLong("institution_connection_id"),
                    rs.getLong("connection_id"),
                    rs.getString("institution_code"),
                    rs.getString("business_type"),
                    rs.getString("status"),
                    rs.getString("login_id_encrypted"),
                    rs.getString("login_password_encrypted"),
                    rs.getString("birth_date_encrypted")),
            userId,
            institutionCode,
            businessType);
    return rows.stream().findFirst();
  }

  public void saveInstitutionConnection(
      long connectionId,
      String institutionCode,
      String businessType,
      String loginType,
      String encryptedLoginId,
      String encryptedPassword,
      String encryptedBirthDate) {
    jdbcTemplate.update(
        "INSERT INTO codef_institution_connection "
            + "(connection_id, institution_code, business_type, login_type, login_id_encrypted, "
            + "login_password_encrypted, birth_date_encrypted, status, last_sync_error_message) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NULL) "
            + "ON DUPLICATE KEY UPDATE login_type = VALUES(login_type), status = 'ACTIVE', "
            + "login_id_encrypted = VALUES(login_id_encrypted), "
            + "login_password_encrypted = VALUES(login_password_encrypted), "
            + "birth_date_encrypted = VALUES(birth_date_encrypted), "
            + "last_sync_error_message = NULL",
        connectionId,
        institutionCode,
        businessType,
        loginType,
        encryptedLoginId,
        encryptedPassword,
        encryptedBirthDate);
  }

  public List<StoredInstitutionConnection> findActiveInstitutionConnectionsByUserId(long userId) {
    return jdbcTemplate.query(
        "SELECT cic.institution_connection_id, cic.connection_id, cic.institution_code,"
            + " cic.business_type, cic.status, cic.login_id_encrypted,"
            + " cic.login_password_encrypted, cic.birth_date_encrypted FROM"
            + " codef_institution_connection cic JOIN codef_connection cc ON cc.connection_id ="
            + " cic.connection_id WHERE cc.user_id = ? AND cc.status = 'ACTIVE' AND cic.status ="
            + " 'ACTIVE' ORDER BY cic.updated_at DESC",
        (rs, rowNum) ->
            new StoredInstitutionConnection(
                rs.getLong("institution_connection_id"),
                rs.getLong("connection_id"),
                rs.getString("institution_code"),
                rs.getString("business_type"),
                rs.getString("status"),
                rs.getString("login_id_encrypted"),
                rs.getString("login_password_encrypted"),
                rs.getString("birth_date_encrypted")),
        userId);
  }

  public void updateInstitutionSyncSuccess(
      long connectionId, String institutionCode, String businessType) {
    jdbcTemplate.update(
        "UPDATE codef_institution_connection SET status = 'ACTIVE', last_sync_at ="
            + " CURRENT_TIMESTAMP, last_sync_error_message = NULL WHERE connection_id = ? AND"
            + " institution_code = ? AND business_type = ?",
        connectionId,
        institutionCode,
        businessType);
  }

  public void updateInstitutionSyncError(
      long connectionId, String institutionCode, String businessType, String message) {
    jdbcTemplate.update(
        "UPDATE codef_institution_connection SET last_sync_error_message = ? "
            + "WHERE connection_id = ? AND institution_code = ? AND business_type = ?",
        truncate(message, 500),
        connectionId,
        institutionCode,
        businessType);
  }

  public void upsertAccount(
      long connectionId,
      String institutionCode,
      String businessType,
      String institutionName,
      String encryptedAccountNumber,
      String accountNumberHash,
      String accountMasked,
      String accountType,
      String productName,
      long currentBalance,
      Long availableBalance,
      LocalDate accountOpenedDate,
      LocalDate maturityDate) {
    jdbcTemplate.update(
        "INSERT INTO connected_account (connection_id, institution_code, business_type,"
            + " institution_name, account_number_encrypted, account_number_hash, account_masked,"
            + " account_type, product_name, current_balance, available_balance,"
            + " account_opened_date, maturity_date, last_synced_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?,"
            + " ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE institution_name ="
            + " VALUES(institution_name), account_masked = VALUES(account_masked), account_type ="
            + " VALUES(account_type), product_name = VALUES(product_name), current_balance ="
            + " VALUES(current_balance), available_balance = VALUES(available_balance),"
            + " account_opened_date = VALUES(account_opened_date), maturity_date ="
            + " VALUES(maturity_date), last_synced_at = CURRENT_TIMESTAMP",
        connectionId,
        institutionCode,
        businessType,
        institutionName,
        encryptedAccountNumber,
        accountNumberHash,
        accountMasked,
        accountType,
        productName,
        currentBalance,
        availableBalance,
        toSqlDate(accountOpenedDate),
        toSqlDate(maturityDate));
  }

  public List<StoredConnectedAccount> findAccountsByUserId(long userId) {
    return jdbcTemplate.query(
        accountSelect() + " WHERE cc.user_id = ? AND ca.status = 'ACTIVE' ORDER BY ca.account_id",
        this::mapAccount,
        userId);
  }

  public List<StoredConnectedAccount> findAccountsByUserIdAndInstitution(
      long userId, String institutionCode) {
    return jdbcTemplate.query(
        accountSelect()
            + " WHERE cc.user_id = ? AND ca.institution_code = ? "
            + "AND ca.status = 'ACTIVE' ORDER BY ca.account_id",
        this::mapAccount,
        userId,
        institutionCode);
  }

  public List<StoredConnectedAccount> findAccountsByUserIdAndInstitution(
      long userId, String institutionCode, String businessType) {
    return jdbcTemplate.query(
        accountSelect()
            + " WHERE cc.user_id = ? AND ca.institution_code = ? AND ca.business_type = ? "
            + "AND ca.status = 'ACTIVE' ORDER BY ca.account_id",
        this::mapAccount,
        userId,
        institutionCode,
        businessType);
  }

  public List<StoredConnectedAccount> findSavingsByUserId(long userId) {
    return jdbcTemplate.query(
        accountSelect()
            + " WHERE cc.user_id = ? AND ca.status = 'ACTIVE' "
            + "AND ca.account_type = 'INSTALLMENT_SAVINGS' ORDER BY ca.account_id",
        this::mapAccount,
        userId);
  }

  public List<StoredInstitutionSyncTarget> findInstitutionSyncTargetsByUserId(long userId) {
    return jdbcTemplate.query(
        "SELECT cic.institution_code, cic.business_type FROM codef_institution_connection cic "
            + "JOIN codef_connection cc ON cc.connection_id = cic.connection_id "
            + "WHERE cc.user_id = ? AND cc.status = 'ACTIVE' AND cic.status = 'ACTIVE'",
        (rs, rowNum) ->
            new StoredInstitutionSyncTarget(
                rs.getString("institution_code"), rs.getString("business_type")),
        userId);
  }

  public Optional<StoredConnectedAccount> findAccountByIdAndUserId(long accountId, long userId) {
    List<StoredConnectedAccount> rows =
        jdbcTemplate.query(
            accountSelect()
                + " WHERE ca.account_id = ? AND cc.user_id = ? AND ca.status = 'ACTIVE'",
            this::mapAccount,
            accountId,
            userId);
    return rows.stream().findFirst();
  }

  /** 실제 데이터를 삭제하지 않고 사용자의 계좌를 조회 대상에서 제외합니다. */
  public int deactivateAccountByIdAndUserId(long accountId, long userId) {
    return jdbcTemplate.update(
        "UPDATE connected_account ca JOIN codef_connection cc ON cc.connection_id ="
            + " ca.connection_id SET ca.status = 'DISCONNECTED' WHERE ca.account_id = ?"
            + " AND cc.user_id = ? AND ca.status = 'ACTIVE'",
        accountId,
        userId);
  }

  /** 사용자가 비활성화한 계좌를 다시 조회 및 분석 대상에 포함합니다. */
  public int activateAccountByIdAndUserId(long accountId, long userId) {
    return jdbcTemplate.update(
        "UPDATE connected_account ca JOIN codef_connection cc ON cc.connection_id ="
            + " ca.connection_id SET ca.status = 'ACTIVE' WHERE ca.account_id = ?"
            + " AND cc.user_id = ? AND cc.status = 'ACTIVE' AND ca.status = 'DISCONNECTED'",
        accountId,
        userId);
  }

  /** 시연 계좌 재연결 시 선택 화면에 다시 노출할 계좌만 활성화합니다. */
  public int activateAccountByConnectionInstitutionAndAccountHash(
      long connectionId, String institutionCode, String accountNumberHash) {
    return jdbcTemplate.update(
        "UPDATE connected_account SET status = 'ACTIVE' WHERE connection_id = ? "
            + "AND institution_code = ? AND account_number_hash = ? AND status = 'DISCONNECTED'",
        connectionId,
        institutionCode,
        accountNumberHash);
  }

  /** 멱등 계좌 upsert 후 로컬에 준비된 계좌를 찾습니다. */
  public Optional<Long> findAccountIdByConnectionInstitutionAndAccountHash(
      long connectionId, String institutionCode, String accountNumberHash) {
    List<Long> accountIds =
        jdbcTemplate.query(
            "SELECT account_id FROM connected_account WHERE connection_id = ? "
                + "AND institution_code = ? AND account_number_hash = ? AND status = 'ACTIVE'",
            (rs, rowNum) -> rs.getLong("account_id"),
            connectionId,
            institutionCode,
            accountNumberHash);
    return accountIds.stream().findFirst();
  }

  /** 시연용 장병내일준비적금만 정리합니다. 실제 CODEF 적금은 유지합니다. */
  public void deleteDemoSoldierSavings(long userId) {
    jdbcTemplate.update(
        "DELETE ca FROM connected_account ca JOIN codef_connection cc ON cc.connection_id ="
            + " ca.connection_id JOIN soldier_saving ss ON ss.account_id = ca.account_id WHERE"
            + " cc.user_id = ? AND ss.source_type = 'DEMO'",
        userId);
  }

  /** 시연 적금은 CODEF 거래내역 동기화 대상에서 제외합니다. */
  public boolean isDemoSoldierSavingAccount(long accountId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM soldier_saving WHERE account_id = ? AND source_type = 'DEMO'",
            Integer.class,
            accountId);
    return count != null && count > 0;
  }

  /** CODEF가 제공하지 않는 계좌 역할을 시연 데이터에만 보완합니다. */
  public void updateAccountRole(long accountId, String accountRole) {
    jdbcTemplate.update(
        "UPDATE connected_account SET account_role = ? WHERE account_id = ?", accountRole, accountId);
  }

  public boolean isTransactionPeriodCovered(
      long accountId, String inquiryType, LocalDate startDate, LocalDate endDate) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM account_transaction_sync WHERE account_id = ? AND inquiry_type ="
                + " ? AND requested_start_date <= ? AND requested_end_date >= ?",
            Integer.class,
            accountId,
            inquiryType,
            toSqlDate(startDate),
            toSqlDate(endDate));
    return count != null && count > 0;
  }

  public void recordTransactionSyncPeriod(
      long accountId, String inquiryType, LocalDate startDate, LocalDate endDate) {
    jdbcTemplate.update(
        "INSERT INTO account_transaction_sync (account_id, inquiry_type, requested_start_date,"
            + " requested_end_date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE synced_at ="
            + " CURRENT_TIMESTAMP",
        accountId,
        inquiryType,
        toSqlDate(startDate),
        toSqlDate(endDate));
  }

  public void insertTransactionIfAbsent(
      long accountId,
      java.time.LocalDateTime transactionAt,
      long amount,
      Long balanceAfter,
      String transactionType,
      String description,
      String externalTransactionKey) {
    jdbcTemplate.update(
        "INSERT IGNORE INTO transaction_history (account_id, transaction_datetime, amount,"
            + " balance_after, transaction_type, transaction_description, external_transaction_key)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?)",
        accountId,
        transactionAt,
        amount,
        balanceAfter,
        transactionType,
        description,
        externalTransactionKey);
  }

  /** 기존 분류(사용자 선택 분류 포함)를 덮어쓰지 않고 규칙 분류를 설정합니다. */
  public void fillTransactionCategoryIfEmpty(
      long accountId, String externalTransactionKey, String category) {
    jdbcTemplate.update(
        "UPDATE transaction_history SET category = COALESCE(category, ?), "
            + "category_source = CASE WHEN category IS NULL THEN 'RULE' ELSE category_source END "
            + "WHERE account_id = ? AND external_transaction_key = ?",
        category,
        accountId,
        externalTransactionKey);
  }

  public void upsertSoldierSaving(
      long userId,
      long accountId,
      String bankName,
      Long monthlyAmount,
      java.math.BigDecimal interestRate,
      LocalDate startDate,
      LocalDate endDate) {
    jdbcTemplate.update(
        "INSERT INTO soldier_saving (user_id, account_id, source_type, bank_name, monthly_amount, "
            + "interest_rate, start_date, end_date) VALUES (?, ?, 'CODEF', ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), bank_name = VALUES(bank_name), "
            + "monthly_amount = VALUES(monthly_amount), interest_rate = VALUES(interest_rate), "
            + "start_date = VALUES(start_date), end_date = VALUES(end_date)",
        userId,
        accountId,
        bankName,
        monthlyAmount,
        interestRate,
        toSqlDate(startDate),
        toSqlDate(endDate));
  }

  /** 시연용 장병내일준비적금은 실제 CODEF 적금과 구분해 저장합니다. */
  public void upsertDemoSoldierSaving(
      long userId,
      long accountId,
      String bankName,
      Long monthlyAmount,
      java.math.BigDecimal interestRate,
      LocalDate startDate,
      LocalDate endDate) {
    jdbcTemplate.update(
        "INSERT INTO soldier_saving (user_id, account_id, source_type, bank_name, monthly_amount, "
            + "interest_rate, start_date, end_date) VALUES (?, ?, 'DEMO', ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), source_type = 'DEMO', "
            + "bank_name = VALUES(bank_name), monthly_amount = VALUES(monthly_amount), "
            + "interest_rate = VALUES(interest_rate), start_date = VALUES(start_date), "
            + "end_date = VALUES(end_date)",
        userId,
        accountId,
        bankName,
        monthlyAmount,
        interestRate,
        toSqlDate(startDate),
        toSqlDate(endDate));
  }

  public List<StoredTransaction> findTransactions(
      long accountId, LocalDate startDate, LocalDate endDate) {
    return jdbcTemplate.query(
        "SELECT transaction_id, transaction_datetime, amount, balance_after, transaction_type, "
            + "category, transaction_description FROM transaction_history "
            + "WHERE account_id = ? AND transaction_datetime >= ? AND transaction_datetime < ? "
            + "ORDER BY transaction_datetime DESC, transaction_id DESC",
        (rs, rowNum) ->
            new StoredTransaction(
                rs.getLong("transaction_id"),
                accountId,
                rs.getObject("transaction_datetime", java.time.LocalDateTime.class),
                rs.getLong("amount"),
                rs.getObject("balance_after", Long.class),
                rs.getString("transaction_type"),
                rs.getString("category"),
        rs.getString("transaction_description")),
        accountId,
        startDate.atStartOfDay(),
        endDate.plusDays(1).atStartOfDay());
  }

  /** 계좌와 분류로 선택 범위를 좁힐 수 있는 사용자의 거래내역만 반환합니다. */
  public List<StoredTransaction> findTransactionsByUser(
      long userId, Long accountId, LocalDate startDate, LocalDate endDate, String category) {
    StringBuilder query =
        new StringBuilder(
            "SELECT th.transaction_id, th.account_id, th.transaction_datetime, th.amount, "
                + "th.balance_after, th.transaction_type, th.category, th.transaction_description "
                + "FROM transaction_history th "
                + "JOIN connected_account ca ON ca.account_id = th.account_id "
                + "JOIN codef_connection cc ON cc.connection_id = ca.connection_id "
                + "WHERE cc.user_id = ? AND ca.status = 'ACTIVE' "
                + "AND th.transaction_datetime >= ? AND th.transaction_datetime < ?");
    java.util.List<Object> parameters = new java.util.ArrayList<>();
    parameters.add(userId);
    parameters.add(startDate.atStartOfDay());
    parameters.add(endDate.plusDays(1).atStartOfDay());
    if (accountId != null) {
      query.append(" AND th.account_id = ?");
      parameters.add(accountId);
    }
    if (category != null && !category.isBlank()) {
      query.append(" AND th.category = ?");
      parameters.add(category);
    }
    query.append(" ORDER BY th.transaction_datetime DESC, th.transaction_id DESC");
    return jdbcTemplate.query(
        query.toString(),
        (rs, rowNum) ->
            new StoredTransaction(
                rs.getLong("transaction_id"),
                rs.getLong("account_id"),
                rs.getObject("transaction_datetime", java.time.LocalDateTime.class),
                rs.getLong("amount"),
                rs.getObject("balance_after", Long.class),
                rs.getString("transaction_type"),
                rs.getString("category"),
                rs.getString("transaction_description")),
        parameters.toArray());
  }

  /** 요청한 사용자에게 속한 거래내역만 수정합니다. */
  public int updateTransactionCategoryByUser(long transactionId, long userId, String category) {
    return jdbcTemplate.update(
        "UPDATE transaction_history th "
            + "JOIN connected_account ca ON ca.account_id = th.account_id "
            + "JOIN codef_connection cc ON cc.connection_id = ca.connection_id "
            + "SET th.category = ?, th.category_source = 'USER' "
            + "WHERE th.transaction_id = ? AND cc.user_id = ? AND ca.status = 'ACTIVE'",
        category,
        transactionId,
        userId);
  }

  private String accountSelect() {
    return "SELECT ca.account_id, cc.user_id, ca.connection_id, ca.institution_code,"
               + " ca.business_type, ca.institution_name, ca.account_number_encrypted,"
               + " ca.account_masked, ca.account_type, ca.product_name, ca.current_balance,"
               + " ca.available_balance, ca.maturity_date FROM connected_account ca JOIN"
               + " codef_connection cc ON cc.connection_id = ca.connection_id";
  }

  private StoredConnectedAccount mapAccount(java.sql.ResultSet rs, int rowNum)
      throws java.sql.SQLException {
    Date maturityDate = rs.getDate("maturity_date");
    Long availableBalance = rs.getObject("available_balance", Long.class);
    return new StoredConnectedAccount(
        rs.getLong("account_id"),
        rs.getLong("user_id"),
        rs.getLong("connection_id"),
        rs.getString("institution_code"),
        rs.getString("business_type"),
        rs.getString("institution_name"),
        rs.getString("account_number_encrypted"),
        rs.getString("account_masked"),
        rs.getString("account_type"),
        rs.getString("product_name"),
        rs.getLong("current_balance"),
        availableBalance,
        maturityDate == null ? null : maturityDate.toLocalDate().toString());
  }

  private Date toSqlDate(LocalDate value) {
    return value == null ? null : Date.valueOf(value);
  }

  private String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
