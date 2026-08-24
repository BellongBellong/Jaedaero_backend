package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.codef.persistence.StoredTransactionCategoryCandidate;
import java.util.List;

public interface TransactionCategoryAiClassifier {

  List<TransactionCategoryClassification> classify(
      List<StoredTransactionCategoryCandidate> candidates);
}
