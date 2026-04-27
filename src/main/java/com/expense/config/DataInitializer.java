package com.expense.config;

import com.expense.model.*;
import com.expense.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds default categories and sample transactions.
 * Java 24: var, List.of (immutable), text blocks for log messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository    categoryRepo;
    private final TransactionRepository transactionRepo;

    @Override
    public void run(String... args) {
        if (categoryRepo.count() > 0) {
            log.info("Data already seeded — skipping initializer.");
            return;
        }

        log.info("Seeding categories and sample transactions…");

        // Java 24: var + List.of (SequencedCollection — supports getFirst/getLast)
        var expenseCategories = List.of(
            cat("Food & Dining",   "#E74C3C", TransactionType.EXPENSE),
            cat("Transportation",  "#3498DB", TransactionType.EXPENSE),
            cat("Shopping",        "#9B59B6", TransactionType.EXPENSE),
            cat("Utilities",       "#F39C12", TransactionType.EXPENSE),
            cat("Healthcare",      "#1ABC9C", TransactionType.EXPENSE),
            cat("Entertainment",   "#E67E22", TransactionType.EXPENSE),
            cat("Rent / Housing",  "#2C3E50", TransactionType.EXPENSE),
            cat("Education",       "#27AE60", TransactionType.EXPENSE)
        );

        var incomeCategories = List.of(
            cat("Salary",        "#27AE60", TransactionType.INCOME),
            cat("Freelance",     "#2ECC71", TransactionType.INCOME),
            cat("Investment",    "#16A085", TransactionType.INCOME),
            cat("Other Income",  "#1ABC9C", TransactionType.INCOME)
        );

        categoryRepo.saveAll(expenseCategories);
        categoryRepo.saveAll(incomeCategories);

        // Resolve saved categories for FK references
        var food    = categoryRepo.findByName("Food & Dining").orElseThrow();
        var transport = categoryRepo.findByName("Transportation").orElseThrow();
        var salary  = categoryRepo.findByName("Salary").orElseThrow();
        var rent    = categoryRepo.findByName("Rent / Housing").orElseThrow();
        var entertain = categoryRepo.findByName("Entertainment").orElseThrow();

        var now = LocalDateTime.now();

        var sampleTransactions = List.of(
            // Income
            txn("Monthly Salary",     "5000.00", TransactionType.INCOME, salary,    now.withDayOfMonth(1)),
            txn("Freelance Project",  "800.00",  TransactionType.INCOME, salary,    now.withDayOfMonth(10)),
            // Normal expenses
            txn("Monthly Rent",       "1200.00", TransactionType.EXPENSE, rent,     now.withDayOfMonth(1)),
            txn("Grocery Shopping",   "87.50",   TransactionType.EXPENSE, food,     now.withDayOfMonth(3)),
            txn("Bus Pass",           "45.00",   TransactionType.EXPENSE, transport,now.withDayOfMonth(4)),
            txn("Restaurant Dinner",  "62.00",   TransactionType.EXPENSE, food,     now.withDayOfMonth(7)),
            txn("Netflix",            "15.99",   TransactionType.EXPENSE, entertain,now.withDayOfMonth(9)),
            txn("Coffee & Snacks",    "23.40",   TransactionType.EXPENSE, food,     now.withDayOfMonth(12)),
            // Suspicious: large amount + 2AM
            txn("Electronics Purchase","2400.00",TransactionType.EXPENSE, entertain,
                now.withDayOfMonth(15).withHour(2).withMinute(34))
        );

        transactionRepo.saveAll(sampleTransactions);
        log.info("Seeded {} categories and {} transactions.", 
            expenseCategories.size() + incomeCategories.size(), sampleTransactions.size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Category cat(String name, String color, TransactionType type) {
        return Category.builder().name(name).color(color).type(type).build();
    }

    private static Transaction txn(String desc, String amount, TransactionType type,
                                    Category category, LocalDateTime date) {
        return Transaction.builder()
            .description(desc)
            .amount(new BigDecimal(amount))
            .type(type)
            .category(category)
            .transactionDate(date)
            .build();
    }
}
