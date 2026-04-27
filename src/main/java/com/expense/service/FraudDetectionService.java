package com.expense.service;

import com.expense.model.*;
import com.expense.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)   // FIX: open session for all reads
public class FraudDetectionService {

    private final TransactionRepository transactionRepo;
    private final FraudAlertRepository  fraudAlertRepo;

    @Value("${fraud.detection.zscore-threshold:2.5}")
    private double zscoreThreshold;

    @Value("${fraud.detection.large-transaction-multiplier:3.0}")
    private double largeTransactionMultiplier;

    @Value("${fraud.detection.suspicious-hour-start:0}")
    private int suspiciousHourStart;

    @Value("${fraud.detection.suspicious-hour-end:5}")
    private int suspiciousHourEnd;

    @Transactional
    public List<FraudAlert> analyzeTransaction(Transaction transaction) {
        if (transaction.getType() != TransactionType.EXPENSE) return List.of();

        var alerts = new ArrayList<FraudAlert>();
        checkLargeAmount(transaction).ifPresent(alerts::add);
        checkUnusualHour(transaction).ifPresent(alerts::add);
        checkCategorySpike(transaction).ifPresent(alerts::add);

        if (!alerts.isEmpty()) {
            var reasons = alerts.stream()
                .map(FraudAlert::getAlertType)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            transaction.setFlaggedAsSuspicious(true);
            transaction.setSuspicionReason(reasons);
            transactionRepo.save(transaction);
            fraudAlertRepo.saveAll(alerts);
        }
        return alerts;
    }

    public List<FraudAlert> getActiveAlerts() {
        return fraudAlertRepo.findByResolvedFalseOrderByDetectedAtDesc();
    }

    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepo.findAllByOrderByDetectedAtDesc();
    }

    @Transactional
    public void resolveAlert(Long alertId) {
        fraudAlertRepo.findById(alertId).ifPresent(alert -> {
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            fraudAlertRepo.save(alert);
        });
    }

    @Transactional
    public void runFullScan() {
        log.info("Running full fraud detection scan...");
        var expenses = transactionRepo.findAllExpensesDesc();
        var flagged  = 0;
        for (var t : expenses) {
            if (fraudAlertRepo.findByTransactionId(t.getId()).isEmpty()) {
                flagged += analyzeTransaction(t).size();
            }
        }
        log.info("Fraud scan complete — {} new alerts.", flagged);
    }

    // ── Checks ───────────────────────────────────────────────────────────────

    private Optional<FraudAlert> checkLargeAmount(Transaction transaction) {
        var avg    = transactionRepo.findAverageAmountByType(TransactionType.EXPENSE);
        var stdDev = transactionRepo.findStdDevAmountByType(TransactionType.EXPENSE);
        if (avg == null || stdDev == null || stdDev == 0) return Optional.empty();

        double amount = transaction.getAmount().doubleValue();
        double mean   = avg.doubleValue();
        double zscore = (amount - mean) / stdDev;
        if (zscore <= zscoreThreshold) return Optional.empty();

        return Optional.of(FraudAlert.builder()
            .transaction(transaction)
            .alertType("LARGE_AMOUNT")
            .description("Amount $%.2f is %.1fx the average ($%.2f). Z-score: %.2f"
                .formatted(amount, amount / mean, mean, zscore))
            .riskScore(Math.min(10.0, zscore * 2))
            .build());
    }

    private Optional<FraudAlert> checkUnusualHour(Transaction transaction) {
        int hour = transaction.getTransactionDate().getHour();
        if (hour < suspiciousHourStart || hour > suspiciousHourEnd) return Optional.empty();

        double risk = switch (hour) {
            case 0, 1 -> 6.5;
            case 2, 3 -> 5.5;
            default   -> 4.0;
        };

        return Optional.of(FraudAlert.builder()
            .transaction(transaction)
            .alertType("UNUSUAL_HOUR")
            .description("Transaction at %02d:00 — outside normal hours (%d:00-%d:00)"
                .formatted(hour, suspiciousHourStart, suspiciousHourEnd))
            .riskScore(risk)
            .build());
    }

    private Optional<FraudAlert> checkCategorySpike(Transaction transaction) {
        if (transaction.getCategory() == null) return Optional.empty();

        var now   = transaction.getTransactionDate();
        var catId = transaction.getCategory().getId();

        double thisMonth = transactionRepo
            .findByCategoryAndYearMonth(catId, now.getYear(), now.getMonthValue())
            .stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();

        var last = now.minusMonths(1);
        double lastMonth = transactionRepo
            .findByCategoryAndYearMonth(catId, last.getYear(), last.getMonthValue())
            .stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();

        if (lastMonth <= 0 || thisMonth <= lastMonth * largeTransactionMultiplier)
            return Optional.empty();

        double ratio = thisMonth / lastMonth;
        return Optional.of(FraudAlert.builder()
            .transaction(transaction)
            .alertType("CATEGORY_SPIKE")
            .description("Category '%s' is %.1fx over last month ($%.2f vs $%.2f)"
                .formatted(transaction.getCategory().getName(), ratio, thisMonth, lastMonth))
            .riskScore(Math.min(10.0, ratio))
            .build());
    }
}
