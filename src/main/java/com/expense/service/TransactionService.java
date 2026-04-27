package com.expense.service;

import com.expense.dto.TransactionDTO;
import com.expense.model.*;
import com.expense.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // FIX: all reads run inside a session
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final CategoryRepository    categoryRepo;
    private final FraudDetectionService fraudService;

    @Transactional   // write operations override readOnly
    public Transaction addTransaction(TransactionDTO dto) {
        var category = dto.categoryId() != null
            ? categoryRepo.findById(dto.categoryId()).orElse(null)
            : null;

        var transaction = Transaction.builder()
            .description(dto.description())
            .amount(dto.amount())
            .type(dto.type())
            .category(category)
            .transactionDate(dto.transactionDate())
            .notes(dto.notes())
            .build();

        var saved = transactionRepo.save(transaction);
        fraudService.analyzeTransaction(saved);
        return saved;
    }

    @Transactional
    public Transaction updateTransaction(Long id, TransactionDTO dto) {
        var existing = transactionRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));

        var category = dto.categoryId() != null
            ? categoryRepo.findById(dto.categoryId()).orElse(null)
            : null;

        existing.setDescription(dto.description());
        existing.setAmount(dto.amount());
        existing.setType(dto.type());
        existing.setCategory(category);
        existing.setTransactionDate(dto.transactionDate());
        existing.setNotes(dto.notes());

        return transactionRepo.save(existing);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        transactionRepo.deleteById(id);
    }

    public List<Transaction> getAllTransactions()                   { return transactionRepo.findAll(); }
    public Optional<Transaction> getById(Long id)                  { return transactionRepo.findById(id); }
    public List<Transaction> getSuspiciousTransactions()           { return transactionRepo.findByFlaggedAsSuspiciousTrue(); }
    public List<Category>    getAllCategories()                     { return categoryRepo.findAll(); }
    public List<Category>    getCategoriesByType(TransactionType t){ return categoryRepo.findByType(t); }

    public List<Transaction> getTransactionsByMonth(int year, int month) {
        return transactionRepo.findByYearAndMonth(year, month);
    }

    public BigDecimal getTotalIncomeForMonth(int year, int month) {
        var total = transactionRepo.sumByTypeAndYearMonth(TransactionType.INCOME, year, month);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalExpensesForMonth(int year, int month) {
        var total = transactionRepo.sumByTypeAndYearMonth(TransactionType.EXPENSE, year, month);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getNetForMonth(int year, int month) {
        return getTotalIncomeForMonth(year, month).subtract(getTotalExpensesForMonth(year, month));
    }
}
