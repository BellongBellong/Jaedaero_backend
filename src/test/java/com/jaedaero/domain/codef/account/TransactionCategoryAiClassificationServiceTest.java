package com.jaedaero.domain.codef.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredTransactionCategoryCandidate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionCategoryAiClassificationServiceTest {

  @Test
  void classifiesInBatchesAndHonorsPerRunLimit() {
    RecordingRepository repository =
        new RecordingRepository(
            List.of(
                candidate(1L), candidate(2L), candidate(3L), candidate(4L)));
    RecordingClassifier classifier = new RecordingClassifier();
    TransactionCategoryAiClassificationService service =
        new TransactionCategoryAiClassificationService(repository, classifier, true, 2, 3);

    int updated = service.classifyPending();

    assertEquals(3, updated);
    assertEquals(List.of(2, 1), classifier.batchSizes);
    assertEquals(List.of(1L, 2L, 3L), repository.updatedIds);
    assertEquals(List.of("FOOD", "FOOD", "FOOD"), repository.updatedCategories);
  }

  @Test
  void doesNothingWhenFeatureIsDisabled() {
    RecordingRepository repository = new RecordingRepository(List.of(candidate(1L)));
    RecordingClassifier classifier = new RecordingClassifier();
    TransactionCategoryAiClassificationService service =
        new TransactionCategoryAiClassificationService(repository, classifier, false, 20, 200);

    assertEquals(0, service.classifyPending());
    assertEquals(List.of(), classifier.batchSizes);
    assertEquals(List.of(), repository.updatedIds);
  }

  private static StoredTransactionCategoryCandidate candidate(long id) {
    return new StoredTransactionCategoryCandidate(id, "분류 대상 " + id);
  }

  private static class RecordingRepository extends CodefPersistenceRepository {
    private final List<StoredTransactionCategoryCandidate> remaining;
    private final List<Long> updatedIds = new ArrayList<>();
    private final List<String> updatedCategories = new ArrayList<>();

    private RecordingRepository(List<StoredTransactionCategoryCandidate> candidates) {
      super(null);
      this.remaining = new ArrayList<>(candidates);
    }

    @Override
    public List<StoredTransactionCategoryCandidate> findRuleUnclassifiedWithdrawals(int limit) {
      return new ArrayList<>(remaining.subList(0, Math.min(limit, remaining.size())));
    }

    @Override
    public int updateTransactionCategoryFromAiIfUnclassified(
        long transactionId, String category) {
      remaining.removeIf(candidate -> candidate.transactionId() == transactionId);
      updatedIds.add(transactionId);
      updatedCategories.add(category);
      return 1;
    }
  }

  private static class RecordingClassifier implements TransactionCategoryAiClassifier {
    private final List<Integer> batchSizes = new ArrayList<>();

    @Override
    public List<TransactionCategoryClassification> classify(
        List<StoredTransactionCategoryCandidate> candidates) {
      batchSizes.add(candidates.size());
      return candidates.stream()
          .map(
              candidate ->
                  new TransactionCategoryClassification(
                      candidate.transactionId(), TransactionCategory.FOOD))
          .toList();
    }
  }
}
