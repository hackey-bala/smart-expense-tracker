package com.expense.ui.panels;

import com.expense.model.Transaction;
import com.expense.service.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

public class ReportPanel extends JPanel {

    private final ReportService reportService;
    private final TransactionService transactionService;
    private final PdfExportService pdfExportService;

    private JComboBox<Integer> yearCombo;
    private JComboBox<Month> monthCombo;
    private JLabel totalIncomeLabel, totalExpenseLabel, netLabel;
    private DefaultTableModel summaryTableModel;
    private ChartPanel trendChartPanel;

    public ReportPanel(ReportService reportService,
                       TransactionService transactionService,
                       PdfExportService pdfExportService) {
        this.reportService       = reportService;
        this.transactionService  = transactionService;
        this.pdfExportService    = pdfExportService;
        initUI();
        loadReport();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(new Color(236, 240, 241));

        // ── Controls ─────────────────────────────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);

        yearCombo = new JComboBox<>();
        int thisYear = LocalDate.now().getYear();
        for (int y = thisYear - 3; y <= thisYear; y++) yearCombo.addItem(y);
        yearCombo.setSelectedItem(thisYear);

        monthCombo = new JComboBox<>(Month.values());
        monthCombo.setSelectedItem(LocalDate.now().getMonth());

        JButton loadBtn  = createButton("📊 Load Report", new Color(41, 128, 185));
        JButton exportBtn = createButton("📄 Export PDF", new Color(231, 76, 60));

        controls.add(new JLabel("Year:"));
        controls.add(yearCombo);
        controls.add(new JLabel("Month:"));
        controls.add(monthCombo);
        controls.add(loadBtn);
        controls.add(exportBtn);

        add(controls, BorderLayout.NORTH);

        // ── Main Content: Split ───────────────────────────────────────────────
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(null);

        // Top: KPI + Category table
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        topPanel.setOpaque(false);

        topPanel.add(createKpiPanel());
        topPanel.add(createCategoryTable());
        splitPane.setTopComponent(topPanel);

        // Bottom: Trend chart
        trendChartPanel = new ChartPanel(createEmptyChart());
        trendChartPanel.setBackground(Color.WHITE);
        trendChartPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        splitPane.setBottomComponent(trendChartPanel);

        add(splitPane, BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────────────
        loadBtn.addActionListener(e -> loadReport());
        exportBtn.addActionListener(e -> exportToPdf());
    }

    private JPanel createKpiPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(16, 20, 16, 20)));

        totalIncomeLabel  = new JLabel("$0.00");
        totalExpenseLabel = new JLabel("$0.00");
        netLabel          = new JLabel("$0.00");

        totalIncomeLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        totalIncomeLabel.setForeground(new Color(39, 174, 96));
        totalExpenseLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        totalExpenseLabel.setForeground(new Color(231, 76, 60));
        netLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        panel.add(createKpiRow("Total Income", totalIncomeLabel));
        panel.add(createKpiRow("Total Expenses", totalExpenseLabel));
        panel.add(createKpiRow("Net Balance", netLabel));
        return panel;
    }

    private JPanel createKpiRow(String title, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLbl.setForeground(new Color(127, 140, 141));
        row.add(titleLbl, BorderLayout.NORTH);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private JPanel createCategoryTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        summaryTableModel = new DefaultTableModel(
            new String[]{"Category", "Amount", "% Share"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(summaryTableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(41, 128, 185));
        table.getTableHeader().setForeground(Color.WHITE);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel header = new JLabel("  Expenses by Category");
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        return panel;
    }

    private void loadReport() {
        int year  = (Integer) yearCombo.getSelectedItem();
        int month = ((Month) monthCombo.getSelectedItem()).getValue();

        Map<String, Object> report = reportService.getMonthlyReport(year, month);

        BigDecimal income  = (BigDecimal) report.get("totalIncome");
        BigDecimal expense = (BigDecimal) report.get("totalExpense");
        BigDecimal net     = (BigDecimal) report.get("netBalance");

        totalIncomeLabel.setText(String.format("$%,.2f", income));
        totalExpenseLabel.setText(String.format("$%,.2f", expense));
        netLabel.setText(String.format("$%,.2f", net));
        netLabel.setForeground(net.compareTo(BigDecimal.ZERO) >= 0
            ? new Color(39, 174, 96) : new Color(231, 76, 60));

        // Category table
        summaryTableModel.setRowCount(0);
        Map<String, BigDecimal> cats = reportService.getCategoryDistribution(year, month);
        BigDecimal total = cats.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        cats.forEach((name, amt) -> {
            double pct = total.compareTo(BigDecimal.ZERO) > 0
                ? amt.doubleValue() / total.doubleValue() * 100 : 0;
            summaryTableModel.addRow(new Object[]{
                name,
                String.format("$%,.2f", amt),
                String.format("%.1f%%", pct)
            });
        });

        // Trend chart
        updateTrendChart();
    }

    private void updateTrendChart() {
        Map<String, BigDecimal> trend = reportService.getMonthlyTrend(6);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        trend.forEach((label, val) -> dataset.addValue(val, "Expenses", label));

        JFreeChart chart = ChartFactory.createLineChart(
            "6-Month Expense Trend", "Month", "Amount ($)",
            dataset, PlotOrientation.VERTICAL, false, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 249, 250));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        plot.setOutlineVisible(false);

        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(231, 76, 60));
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setDefaultShapesVisible(true);

        trendChartPanel.setChart(chart);
    }

    private JFreeChart createEmptyChart() {
        return ChartFactory.createLineChart("Loading...", "", "",
            new DefaultCategoryDataset(), PlotOrientation.VERTICAL, false, false, false);
    }

    private void exportToPdf() {
        int year  = (Integer) yearCombo.getSelectedItem();
        int month = ((Month) monthCombo.getSelectedItem()).getValue();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                return pdfExportService.exportMonthlyReport(year, month);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String path = get();
                    JOptionPane.showMessageDialog(ReportPanel.this,
                        "PDF exported successfully!\n" + path,
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReportPanel.this,
                        "Export failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return btn;
    }
}
