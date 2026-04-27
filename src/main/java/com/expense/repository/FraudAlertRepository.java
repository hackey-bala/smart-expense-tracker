package com.expense.repository;

import com.expense.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByResolvedFalseOrderByDetectedAtDesc();
    List<FraudAlert> findAllByOrderByDetectedAtDesc();
    List<FraudAlert> findByTransactionId(Long transactionId);
}
