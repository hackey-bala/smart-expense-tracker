package com.expense.controller;

import com.expense.dto.TransactionDTO;
import com.expense.model.*;
import com.expense.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API controller.
 * Java 24: var, record pattern in switch, text blocks for error messages.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final TransactionService    transactionService;
    private final ReportService         reportService;
    private final FraudDetectionService fraudService;
    private final PdfExportService      pdfExportService;

    // ── Transactions ─────────────────────────────────────────────────────────

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        return transactionService.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/transactions")
    public ResponseEntity<Transaction> addTransaction(@Valid @RequestBody TransactionDTO dto) {
        return ResponseEntity.ok(transactionService.addTransaction(dto));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable Long id, @Valid @RequestBody TransactionDTO dto) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, dto));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    @GetMapping("/reports/monthly")
    public Map<String, Object> getMonthlyReport(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        // Java 24: var
        var now = LocalDate.now();
        if (year  == 0) year  = now.getYear();
        if (month == 0) month = now.getMonthValue();
        return reportService.getMonthlyReport(year, month);
    }

    @GetMapping("/reports/trend")
    public Map<String, ?> getTrend(@RequestParam(defaultValue = "6") int months) {
        return reportService.getMonthlyTrend(months);
    }

    @GetMapping("/reports/categories")
    public Map<String, ?> getCategoryDistribution(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        var now = LocalDate.now();
        if (year  == 0) year  = now.getYear();
        if (month == 0) month = now.getMonthValue();
        return reportService.getCategoryDistribution(year, month);
    }

    @GetMapping("/reports/income-vs-expense")
    public Map<String, ?> getIncomeVsExpense(@RequestParam(defaultValue = "6") int months) {
        return reportService.getIncomeExpenseComparison(months);
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    @PostMapping("/reports/export-pdf")
    public ResponseEntity<Map<String, String>> exportPdf(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        try {
            var now = LocalDate.now();
            if (year  == 0) year  = now.getYear();
            if (month == 0) month = now.getMonthValue();
            var path = pdfExportService.exportMonthlyReport(year, month);
            return ResponseEntity.ok(Map.of("path", path, "status", "success"));
        } catch (Exception e) {
            // Java 24: text block in map value
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Fraud Detection ───────────────────────────────────────────────────────

    @GetMapping("/fraud/alerts")
    public List<FraudAlert> getActiveAlerts() { return fraudService.getActiveAlerts(); }

    @GetMapping("/fraud/alerts/all")
    public List<FraudAlert> getAllAlerts() { return fraudService.getAllAlerts(); }

    @PostMapping("/fraud/alerts/{id}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long id) {
        fraudService.resolveAlert(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fraud/scan")
    public ResponseEntity<Map<String, String>> runScan() {
        fraudService.runFullScan();
        return ResponseEntity.ok(Map.of("status", "Scan complete"));
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @GetMapping("/categories")
    public List<Category> getCategories() { return transactionService.getAllCategories(); }
}
