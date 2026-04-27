package com.expense.repository;

import com.expense.model.Transaction;
import com.expense.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTypeOrderByTransactionDateDesc(TransactionType type);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
        LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE YEAR(t.transactionDate) = :year " +
           "AND MONTH(t.transactionDate) = :month ORDER BY t.transactionDate DESC")
    List<Transaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    BigDecimal sumByTypeAndYearMonth(@Param("type") TransactionType type,
                                     @Param("year") int year,
                                     @Param("month") int month);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    List<Transaction> findByTypeAndYearMonth(@Param("type") TransactionType type,
                                              @Param("year") int year,
                                              @Param("month") int month);

    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.type = :type")
    BigDecimal findAverageAmountByType(@Param("type") TransactionType type);

    @Query("SELECT STDDEV(t.amount) FROM Transaction t WHERE t.type = :type")
    Double findStdDevAmountByType(@Param("type") TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE t.type = 'EXPENSE' " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findAllExpensesDesc();

    List<Transaction> findByFlaggedAsSuspiciousTrue();

    @Query("SELECT t FROM Transaction t WHERE t.category.id = :categoryId " +
           "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    List<Transaction> findByCategoryAndYearMonth(@Param("categoryId") Long categoryId,
                                                  @Param("year") int year,
                                                  @Param("month") int month);
}
