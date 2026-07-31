package com.jaedaero.domain.codef.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class TransactionControllerTest {

  @Test
  void filtersTransactionsByAuthenticatedUserAccountPeriodAndCategory() {
    CapturingRepository repository = new CapturingRepository();
    TransactionController controller = new TransactionController(repository);
    LocalDate startDate = LocalDate.of(2026, 4, 1);
    LocalDate endDate = LocalDate.of(2026, 4, 30);

    List<TransactionResponse> response =
        controller.getTransactions(authentication(7), 10L, startDate, endDate, "식비");

    assertEquals(7L, repository.userId);
    assertEquals(10L, repository.accountId);
    assertEquals(startDate, repository.startDate);
    assertEquals(endDate, repository.endDate);
    assertEquals("식비", repository.category);
    assertEquals(1, response.size());
    assertEquals(10L, response.get(0).getAccountId());
  }

  @Test
  void allowsAllAccountsWhenAccountIdAndCategoryAreOmitted() {
    CapturingRepository repository = new CapturingRepository();
    TransactionController controller = new TransactionController(repository);
    LocalDate startDate = LocalDate.of(2026, 7, 1);
    LocalDate endDate = LocalDate.of(2026, 7, 30);

    controller.getTransactions(authentication(1), null, startDate, endDate, null);

    assertEquals(1L, repository.userId);
    assertNull(repository.accountId);
    assertNull(repository.category);
  }

  @Test
  void rejectsAReversedPeriod() {
    TransactionController controller = new TransactionController(new CapturingRepository());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.getTransactions(
                    authentication(1),
                    10L,
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 7, 1),
                    null));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void rejectsUnauthenticatedRequests() {
    TransactionController controller = new TransactionController(new CapturingRepository());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.getTransactions(
                    null, 10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30), null));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }

  @Test
  void updatesCategoryOnlyForTheAuthenticatedUsersTransaction() {
    CapturingRepository repository = new CapturingRepository();
    TransactionController controller = new TransactionController(repository);
    TransactionCategoryUpdateRequest request = new TransactionCategoryUpdateRequest();
    request.setCategory(TransactionCategory.FOOD);

    TransactionCategoryUpdateResponse response =
        controller.updateTransactionCategory(authentication(7), 100L, request);

    assertEquals(7L, repository.updatedUserId);
    assertEquals(100L, repository.updatedTransactionId);
    assertEquals("FOOD", repository.updatedCategory);
    assertEquals(TransactionCategory.FOOD, response.getCategory());
    assertEquals("식비", response.getCategoryName());
  }

  @Test
  void returnsNotFoundWhenTheTransactionDoesNotBelongToTheUser() {
    CapturingRepository repository = new CapturingRepository();
    repository.updateCount = 0;
    TransactionController controller = new TransactionController(repository);
    TransactionCategoryUpdateRequest request = new TransactionCategoryUpdateRequest();
    request.setCategory(TransactionCategory.ETC);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.updateTransactionCategory(authentication(7), 100L, request));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

  private UsernamePasswordAuthenticationToken authentication(long userId) {
    return new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of());
  }

  private static class CapturingRepository extends CodefPersistenceRepository {
    private long userId;
    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String category;
    private long updatedUserId;
    private long updatedTransactionId;
    private String updatedCategory;
    private int updateCount = 1;

    private CapturingRepository() {
      super(null);
    }

    @Override
    public List<StoredTransaction> findTransactionsByUser(
        long userId, Long accountId, LocalDate startDate, LocalDate endDate, String category) {
      this.userId = userId;
      this.accountId = accountId;
      this.startDate = startDate;
      this.endDate = endDate;
      this.category = category;
      return List.of(
          new StoredTransaction(
              100L,
              accountId == null ? 10L : accountId,
              LocalDateTime.of(2026, 7, 1, 12, 0),
              5000L,
              95000L,
              "WITHDRAW",
              category,
              "테스트 거래"));
    }

    @Override
    public int updateTransactionCategoryByUser(long transactionId, long userId, String category) {
      this.updatedTransactionId = transactionId;
      this.updatedUserId = userId;
      this.updatedCategory = category;
      return updateCount;
    }
  }
}
