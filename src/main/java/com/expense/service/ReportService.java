package com.expense.service;

import com.expense.model.TransactionType;
import com.expense.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // FIX: open session for all report queries
public class ReportService {

    private final TransactionRepository transactionRepo;

    public Map<String, Object> getMonthlyReport(int year, int month) {
        var transactions = transactionRepo.findByYearAndMonth(year, month);

        var totalIncome = transactions.stream()
            .filter(t -> t.getType() == TransactionType.INCOME)
            .map(t -> t.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        var totalExpense = transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .map(t -> t.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        var expenseByCategory = transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory() != null)
            .collect(Collectors.groupingBy(
                t -> t.getCategory().getName(),
                Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount(), BigDecimal::add)
            ));

        var topCategory = expenseByCategory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        var suspiciousCount = transactions.stream()
            .filter(t -> t.isFlaggedAsSuspicious())
            .count();

        var report = new LinkedHashMap<String, Object>();
        report.put("year",             year);
        report.put("month",            Month.of(month).name());
        report.put("totalIncome",      totalIncome);
        report.put("totalExpense",     totalExpense);
        report.put("netBalance",       totalIncome.subtract(totalExpense));
        report.put("transactionCount", transactions.size());
        report.put("expenseByCategory",expenseByCategory);
        report.put("topCategory",      topCategory);
        report.put("suspiciousCount",  suspiciousCount);
        return report;
    }

    public Map<String, BigDecimal> getMonthlyTrend(int months) {
        var trend   = new LinkedHashMap<String, BigDecimal>();
        var current = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            var date  = current.minusMonths(i);
            var total = transactionRepo.sumByTypeAndYearMonth(
                TransactionType.EXPENSE, date.getYear(), date.getMonthValue());
            trend.put(date.getMonth().name().substring(0, 3) + " " + date.getYear(),
                      total != null ? total : BigDecimal.ZERO);
        }
        return trend;
    }

    public Map<String, BigDecimal> getCategoryDistribution(int year, int month) {
        var expenses = transactionRepo.findByTypeAndYearMonth(
            TransactionType.EXPENSE, year, month);

        var distribution = new LinkedHashMap<String, BigDecimal>();
        for (var t : expenses) {
            var catName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
            distribution.merge(catName, t.getAmount(), BigDecimal::add);
        }

        return distribution.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new));
    }

    public Map<String, Map<String, BigDecimal>> getIncomeExpenseComparison(int months) {
        var comparison = new LinkedHashMap<String, Map<String, BigDecimal>>();
        var current    = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            var date    = current.minusMonths(i);
            var key     = date.getMonth().name().substring(0, 3);
            var income  = transactionRepo.sumByTypeAndYearMonth(
                TransactionType.INCOME,  date.getYear(), date.getMonthValue());
            var expense = transactionRepo.sumByTypeAndYearMonth(
                TransactionType.EXPENSE, date.getYear(), date.getMonthValue());
            var values  = new LinkedHashMap<String, BigDecimal>();
            values.put("income",  income  != null ? income  : BigDecimal.ZERO);
            values.put("expense", expense != null ? expense : BigDecimal.ZERO);
            comparison.put(key, values);
        }
        return comparison;
    }
}
