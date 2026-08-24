package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredTransactionCategoryCandidate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 일일 CODEF 동기화 뒤 RULE 미분류 출금 거래를 제한된 배치로 AI 분류합니다. */
@Slf4j
@Service
public class TransactionCategoryAiClassificationService {

  private final CodefPersistenceRepository repository;
  private final TransactionCategoryAiClassifier classifier;
  private final boolean enabled;
  private final int batchSize;
  private final int maxItemsPerRun;

  public TransactionCategoryAiClassificationService(
      CodefPersistenceRepository repository,
      TransactionCategoryAiClassifier classifier,
      @Value("${transaction-category.ai.enabled:true}") boolean enabled,
      @Value("${transaction-category.ai.batch-size:20}") int batchSize,
      @Value("${transaction-category.ai.max-items-per-run:200}") int maxItemsPerRun) {
    this.repository = repository;
    this.classifier = classifier;
    this.enabled = enabled;
    this.batchSize = Math.max(1, batchSize);
    this.maxItemsPerRun = Math.max(0, maxItemsPerRun);
  }

  public int classifyPending() {
    if (!enabled || maxItemsPerRun == 0) {
      return 0;
    }

    int inspected = 0;
    int updated = 0;
    while (inspected < maxItemsPerRun) {
      int limit = Math.min(batchSize, maxItemsPerRun - inspected);
      List<StoredTransactionCategoryCandidate> candidates =
          repository.findRuleUnclassifiedWithdrawals(limit);
      if (candidates.isEmpty()) {
        break;
      }

      List<TransactionCategoryClassification> classifications;
      try {
        classifications = classifier.classify(candidates);
      } catch (RuntimeException exception) {
        log.warn(
            "거래 카테고리 AI 분류 배치에 실패해 다음 일일 동기화에서 재시도합니다. candidateCount={}",
            candidates.size(),
            exception);
        break;
      }

      for (TransactionCategoryClassification classification : classifications) {
        updated +=
            repository.updateTransactionCategoryFromAiIfUnclassified(
                classification.transactionId(), classification.category().name());
      }
      inspected += candidates.size();
    }
    if (updated > 0) {
      log.info("거래 카테고리 AI 분류를 완료했습니다. inspected={}, updated={}", inspected, updated);
    }
    return updated;
  }
}
