package com.expense.service;

import com.expense.model.Transaction;
import com.expense.model.TransactionType;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private final TransactionService transactionService;
    private final ReportService reportService;

    @Value("${pdf.export.path:./exports/}")
    private String exportPath;

    // Brand Colors
    private static final DeviceRgb PRIMARY_DARK   = new DeviceRgb(26,  42,  58);
    private static final DeviceRgb PRIMARY_BLUE   = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb INCOME_GREEN   = new DeviceRgb(39, 174,  96);
    private static final DeviceRgb EXPENSE_RED    = new DeviceRgb(231, 76,  60);
    private static final DeviceRgb WARNING_ORANGE = new DeviceRgb(230, 126,  34);
    private static final DeviceRgb LIGHT_GRAY_BG  = new DeviceRgb(245, 247, 250);
    private static final DeviceRgb MID_GRAY       = new DeviceRgb(189, 195, 199);
    private static final DeviceRgb TEXT_DARK      = new DeviceRgb(44,  62,  80);
    private static final DeviceRgb TEXT_MUTED     = new DeviceRgb(127, 140, 141);
    private static final DeviceRgb STRIPE_ODD     = new DeviceRgb(248, 249, 252);
    private static final DeviceRgb STRIPE_EVEN    = new DeviceRgb(255, 255, 255);

    public String exportMonthlyReport(int year, int month) throws IOException {
        new File(exportPath).mkdirs();
        String fileName = String.format("%sExpenseReport_%d_%02d.pdf", exportPath, year, month);

        try (PdfWriter writer = new PdfWriter(fileName);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            document.setMargins(0, 0, 36, 0);

            addHeroHeader(document, year, month);
            addMetaLine(document);
            document.add(spacer(16));
            addSummarySection(document, year, month);
            document.add(spacer(16));
            addCategoryBreakdown(document, year, month);
            document.add(spacer(16));
            addTransactionTable(document, year, month);
            document.add(spacer(24));
            addFooter(document);
        }

        log.info("PDF exported to: {}", fileName);
        return fileName;
    }

    private void addHeroHeader(Document doc, int year, int month) {
        Table hero = new Table(1).useAllAvailableWidth();
        String monthName = Month.of(month).getDisplayName(
            java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH).toUpperCase();

        Cell cell = new Cell()
            .add(new Paragraph("SMART EXPENSE TRACKER")
                .setFontSize(26).setBold().setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4))
            .add(new Paragraph(monthName + " " + year + "  \u00b7  FINANCIAL REPORT")
                .setFontSize(11).setFontColor(new DeviceRgb(189, 215, 238))
                .setTextAlignment(TextAlignment.CENTER).setCharacterSpacing(1.5f))
            .setBackgroundColor(PRIMARY_BLUE)
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(28).setPaddingBottom(28)
            .setPaddingLeft(50).setPaddingRight(50);

        hero.addCell(cell);
        doc.add(hero);
    }

    private void addMetaLine(Document doc) {
        String generated = "Generated  " + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMMM d, yyyy  \u2022  HH:mm"));

        Table bar = new Table(new float[]{1, 1}).useAllAvailableWidth();
        bar.addCell(new Cell()
            .add(new Paragraph("Powered by Spring Boot  +  AI Fraud Detection")
                .setFontSize(8).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(PRIMARY_DARK)
            .setBorder(Border.NO_BORDER)
            .setPaddingLeft(50).setPaddingTop(6).setPaddingBottom(6));
        bar.addCell(new Cell()
            .add(new Paragraph(generated).setFontSize(8)
                .setFontColor(new DeviceRgb(149, 165, 166))
                .setTextAlignment(TextAlignment.RIGHT))
            .setBackgroundColor(PRIMARY_DARK)
            .setBorder(Border.NO_BORDER)
            .setPaddingRight(50).setPaddingTop(6).setPaddingBottom(6));
        doc.add(bar);
    }

    private void addSummarySection(Document doc, int year, int month) {
        Map<String, Object> report = reportService.getMonthlyReport(year, month);
        doc.add(sectionHeading("FINANCIAL SUMMARY"));

        Table kpiRow = new Table(new float[]{1, 1, 1}).useAllAvailableWidth()
            .setMarginLeft(36).setMarginRight(36);

        BigDecimal income  = castBD(report.get("totalIncome"));
        BigDecimal expense = castBD(report.get("totalExpense"));
        BigDecimal net     = castBD(report.get("netBalance"));

        addKpiCard(kpiRow, "TOTAL INCOME",   fmt(income),  INCOME_GREEN, "\u25b2");
        addKpiCard(kpiRow, "TOTAL EXPENSES", fmt(expense), EXPENSE_RED, "\u25bc");
        addKpiCard(kpiRow, "NET BALANCE",    fmt(net),
            net.compareTo(BigDecimal.ZERO) >= 0 ? INCOME_GREEN : EXPENSE_RED, "=");
        doc.add(kpiRow);
        doc.add(spacer(10));

        Table stats = new Table(new float[]{1, 1, 1}).useAllAvailableWidth()
            .setMarginLeft(36).setMarginRight(36);
        addStatChip(stats, "Transactions",  report.get("transactionCount").toString(), PRIMARY_BLUE);
        addStatChip(stats, "Top Category",  report.get("topCategory").toString(),      PRIMARY_BLUE);
        addStatChip(stats, "Suspicious",    report.get("suspiciousCount") + " flagged", WARNING_ORANGE);
        doc.add(stats);
    }

    private void addKpiCard(Table table, String label, String value, DeviceRgb color, String icon) {
        Cell card = new Cell()
            .add(new Paragraph(label).setFontSize(7.5f).setFontColor(TEXT_MUTED)
                .setCharacterSpacing(0.8f).setMarginBottom(6))
            .add(new Paragraph(icon + "  $" + value).setFontSize(18).setBold().setFontColor(color))
            .setBackgroundColor(LIGHT_GRAY_BG)
            .setBorder(new SolidBorder(MID_GRAY, 0.5f))
            .setBorderLeft(new SolidBorder(color, 4))
            .setPadding(14)
            .setMargin(4);
        table.addCell(card);
    }

    private void addStatChip(Table table, String label, String value, DeviceRgb color) {
        Cell chip = new Cell()
            .add(new Paragraph(label).setFontSize(8).setFontColor(TEXT_MUTED).setMarginBottom(2))
            .add(new Paragraph(value).setFontSize(11).setBold().setFontColor(color))
            .setBackgroundColor(STRIPE_EVEN)
            .setBorder(new SolidBorder(MID_GRAY, 0.5f))
            .setPadding(10)
            .setMargin(4);
        table.addCell(chip);
    }

    private void addCategoryBreakdown(Document doc, int year, int month) {
        Map<String, BigDecimal> categories = reportService.getCategoryDistribution(year, month);
        if (categories.isEmpty()) return;

        doc.add(sectionHeading("EXPENSES BY CATEGORY"));
        BigDecimal total = categories.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Table catTable = new Table(new float[]{5, 2.2f, 1.8f, 3})
            .useAllAvailableWidth().setMarginLeft(36).setMarginRight(36);

        addTH(catTable, "Category");
        addTH(catTable, "Amount");
        addTH(catTable, "Share");
        addTH(catTable, "Visual");

        boolean alt = false;
        for (Map.Entry<String, BigDecimal> entry : categories.entrySet()) {
            DeviceRgb bg = alt ? STRIPE_ODD : STRIPE_EVEN;
            double pct = total.compareTo(BigDecimal.ZERO) > 0
                ? entry.getValue().doubleValue() / total.doubleValue() * 100 : 0;

            catTable.addCell(tdCell(entry.getKey(), bg, TextAlignment.LEFT));
            catTable.addCell(tdCell("$" + fmt(entry.getValue()), bg, TextAlignment.RIGHT));
            catTable.addCell(tdCell(String.format("%.1f%%", pct), bg, TextAlignment.CENTER));

            int barFilled = Math.max(1, (int) Math.round(pct / 5));
            String bar = "\u2588".repeat(barFilled) + "\u2591".repeat(20 - barFilled);
            catTable.addCell(new Cell()
                .add(new Paragraph(bar).setFontSize(6).setFontColor(PRIMARY_BLUE))
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(MID_GRAY, 0.3f))
                .setPadding(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
            alt = !alt;
        }
        doc.add(catTable);
    }

    private void addTransactionTable(Document doc, int year, int month) {
        List<Transaction> transactions = transactionService.getTransactionsByMonth(year, month);
        if (transactions.isEmpty()) return;

        doc.add(sectionHeading("ALL TRANSACTIONS  (" + transactions.size() + ")"));

        Table table = new Table(new float[]{2.2f, 4.5f, 2f, 1.8f, 0.8f})
            .useAllAvailableWidth().setMarginLeft(36).setMarginRight(36);

        addTH(table, "Date");
        addTH(table, "Description");
        addTH(table, "Category");
        addTH(table, "Amount");
        addTH(table, "\u26F3");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM d, HH:mm");
        boolean alt = false;

        for (Transaction t : transactions) {
            DeviceRgb bg = alt ? STRIPE_ODD : STRIPE_EVEN;
            boolean isIncome = t.getType() == TransactionType.INCOME;
            DeviceRgb amtColor = isIncome ? INCOME_GREEN : EXPENSE_RED;

            table.addCell(tdCell(t.getTransactionDate().format(dtf), bg, TextAlignment.LEFT));
            table.addCell(tdCell(truncate(t.getDescription(), 40), bg, TextAlignment.LEFT));
            table.addCell(tdCell(
                t.getCategory() != null ? t.getCategory().getName() : "\u2014", bg, TextAlignment.LEFT));

            String sign = isIncome ? "+" : "-";
            table.addCell(new Cell()
                .add(new Paragraph(sign + "$" + fmt(t.getAmount()))
                    .setFontSize(9).setBold().setFontColor(amtColor)
                    .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(MID_GRAY, 0.3f))
                .setPadding(6));

            String flag = t.isFlaggedAsSuspicious() ? "\u26a0" : "";
            table.addCell(new Cell()
                .add(new Paragraph(flag).setFontSize(9).setFontColor(WARNING_ORANGE)
                    .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(MID_GRAY, 0.3f))
                .setPadding(6));

            alt = !alt;
        }
        doc.add(table);
    }

    private void addFooter(Document doc) {
        Table footer = new Table(1).useAllAvailableWidth();
        footer.addCell(new Cell()
            .add(new Paragraph(
                "Smart Expense Tracker  \u00b7  Confidential Financial Report  \u00b7  Generated "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
                .setFontSize(7.5f).setFontColor(new DeviceRgb(149, 165, 166))
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(PRIMARY_DARK)
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(12).setPaddingBottom(12));
        doc.add(footer);
    }

    // Helpers
    private Paragraph sectionHeading(String text) {
        return new Paragraph(text).setFontSize(9).setBold().setFontColor(PRIMARY_BLUE)
            .setCharacterSpacing(1.2f).setMarginLeft(40).setMarginBottom(6).setMarginTop(4);
    }

    private void addTH(Table table, String text) {
        table.addHeaderCell(new Cell()
            .add(new Paragraph(text).setFontSize(8.5f).setBold().setFontColor(ColorConstants.WHITE)
                .setCharacterSpacing(0.5f))
            .setBackgroundColor(TEXT_DARK)
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(8).setPaddingRight(8));
    }

    private Cell tdCell(String text, DeviceRgb bg, TextAlignment align) {
        return new Cell()
            .add(new Paragraph(text).setFontSize(9).setFontColor(TEXT_DARK).setTextAlignment(align))
            .setBackgroundColor(bg)
            .setBorder(new SolidBorder(MID_GRAY, 0.3f))
            .setPadding(6);
    }

    private Paragraph spacer(float height) {
        return new Paragraph("").setMarginBottom(height).setBorder(Border.NO_BORDER);
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount.abs());
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private BigDecimal castBD(Object o) {
        return o instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
    }
}
