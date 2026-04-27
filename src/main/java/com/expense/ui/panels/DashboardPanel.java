package com.expense.ui.panels;

import com.expense.service.ReportService;
import com.expense.service.TransactionService;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

public class DashboardPanel extends JPanel {

    private final TransactionService transactionService;
    private final ReportService reportService;

    private JLabel incomeLabel, expenseLabel, balanceLabel, txCountLabel;
    private ChartPanel pieChartPanel, barChartPanel;

    private final int currentYear  = LocalDate.now().getYear();
    private final int currentMonth = LocalDate.now().getMonthValue();

    // Design tokens
    private static final Color BG          = new Color(242, 245, 250);
    private static final Color WHITE        = Color.WHITE;
    private static final Color INCOME_CLR   = new Color(39, 174, 96);
    private static final Color EXPENSE_CLR  = new Color(231, 76, 60);
    private static final Color BALANCE_CLR  = new Color(41, 128, 185);
    private static final Color PURPLE_CLR   = new Color(142, 68, 173);
    private static final Color BORDER_CLR   = new Color(220, 225, 232);
    private static final Color TEXT_MUTED   = new Color(127, 140, 141);

    public DashboardPanel(TransactionService transactionService, ReportService reportService) {
        this.transactionService = transactionService;
        this.reportService      = reportService;
        initUI();
        refresh();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // ── Page title ────────────────────────────────────────────────────
        JPanel pageTitle = new JPanel(new BorderLayout());
        pageTitle.setOpaque(false);
        pageTitle.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel titleLbl = new JLabel("Dashboard");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLbl.setForeground(new Color(44, 62, 80));
        JLabel subLbl = new JLabel(
            Month.of(currentMonth).getDisplayName(java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH) + " " + currentYear + "  overview");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(TEXT_MUTED);

        pageTitle.add(titleLbl, BorderLayout.WEST);
        JButton refresh = pill("↻  Refresh", BALANCE_CLR);
        refresh.addActionListener(e -> refresh());
        pageTitle.add(refresh, BorderLayout.EAST);
        add(pageTitle, BorderLayout.NORTH);

        // ── KPI row ──────────────────────────────────────────────────────
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiPanel.setOpaque(false);

        incomeLabel  = kpiValue(INCOME_CLR);
        expenseLabel = kpiValue(EXPENSE_CLR);
        balanceLabel = kpiValue(BALANCE_CLR);
        txCountLabel = kpiValue(PURPLE_CLR);

        kpiPanel.add(kpiCard("Total Income",    incomeLabel,  INCOME_CLR,  "\uD83D\uDCB9"));
        kpiPanel.add(kpiCard("Total Expenses",  expenseLabel, EXPENSE_CLR, "\uD83D\uDCB8"));
        kpiPanel.add(kpiCard("Net Balance",     balanceLabel, BALANCE_CLR, "\uD83D\uDCCA"));
        kpiPanel.add(kpiCard("Transactions",    txCountLabel, PURPLE_CLR,  "\uD83D\uDCB3"));

        // ── Charts ────────────────────────────────────────────────────────
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        chartsPanel.setOpaque(false);

        pieChartPanel = new ChartPanel(createEmptyPie());
        barChartPanel = new ChartPanel(createEmptyBar());
        styleChart(pieChartPanel);
        styleChart(barChartPanel);

        chartsPanel.add(pieChartPanel);
        chartsPanel.add(barChartPanel);

        // ── Layout ────────────────────────────────────────────────────────
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 14));
        center.setOpaque(false);
        center.add(kpiPanel);
        center.add(chartsPanel);
        add(center, BorderLayout.CENTER);
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            try {
                Map<String, Object> report = reportService.getMonthlyReport(currentYear, currentMonth);

                BigDecimal income  = (BigDecimal) report.get("totalIncome");
                BigDecimal expense = (BigDecimal) report.get("totalExpense");
                BigDecimal balance = (BigDecimal) report.get("netBalance");
                long count = ((Number) report.get("transactionCount")).longValue();

                incomeLabel.setText(String.format("$%,.2f", income));
                expenseLabel.setText(String.format("$%,.2f", expense));
                balanceLabel.setText(String.format("$%,.2f", balance));
                txCountLabel.setText(String.valueOf(count));
                balanceLabel.setForeground(
                    balance.compareTo(BigDecimal.ZERO) >= 0 ? INCOME_CLR : EXPENSE_CLR);

                updatePieChart();
                updateBarChart();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error loading dashboard: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updatePieChart() {
        Map<String, BigDecimal> cats = reportService.getCategoryDistribution(currentYear, currentMonth);
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        cats.forEach((k, v) -> ds.setValue(k, v.doubleValue()));

        JFreeChart chart = ChartFactory.createPieChart(
            "Expenses by Category", ds, true, true, false);
        chart.setBackgroundPaint(WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 13));
        chart.getTitle().setPaint(new Color(44, 62, 80));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.setShadowPaint(null);
        plot.setLabelBackgroundPaint(new Color(250, 250, 250));
        plot.setLabelOutlinePaint(new Color(200, 200, 200));
        plot.setLabelShadowPaint(null);

        pieChartPanel.setChart(chart);
    }

    private void updateBarChart() {
        Map<String, Map<String, BigDecimal>> comparison = reportService.getIncomeExpenseComparison(6);
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        comparison.forEach((m, vals) -> {
            ds.addValue(vals.get("income"),  "Income",   m);
            ds.addValue(vals.get("expense"), "Expenses", m);
        });

        JFreeChart chart = ChartFactory.createBarChart(
            "Income vs Expenses (6 months)", "Month", "Amount ($)",
            ds, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 13));
        chart.getTitle().setPaint(new Color(44, 62, 80));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 249, 252));
        plot.setRangeGridlinePaint(new Color(220, 225, 232));
        plot.setOutlineVisible(false);

        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, INCOME_CLR);
        r.setSeriesPaint(1, EXPENSE_CLR);
        r.setMaximumBarWidth(0.3);
        r.setShadowVisible(false);
        r.setItemMargin(0.05);

        NumberAxis axis = (NumberAxis) plot.getRangeAxis();
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        axis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        barChartPanel.setChart(chart);
    }

    // ── Component builders ────────────────────────────────────────────────────
    private JPanel kpiCard(String title, JLabel valueLbl, Color accent, String icon) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight(), 6, 6);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 12),
            new EmptyBorder(16, 20, 16, 16)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 18));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLbl.setForeground(TEXT_MUTED);

        top.add(titleLbl, BorderLayout.CENTER);
        top.add(iconLbl,  BorderLayout.EAST);

        card.add(top,      BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
    }

    private JLabel kpiValue(Color color) {
        JLabel lbl = new JLabel("$0.00");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        lbl.setForeground(color);
        return lbl;
    }

    private void styleChart(ChartPanel cp) {
        cp.setBackground(WHITE);
        cp.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 12),
            new EmptyBorder(8, 8, 8, 8)));
    }

    private JButton pill(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JFreeChart createEmptyPie() {
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        ds.setValue("Loading...", 1);
        return ChartFactory.createPieChart("Loading...", ds, false, false, false);
    }

    private JFreeChart createEmptyBar() {
        return ChartFactory.createBarChart("Loading...", "", "",
            new DefaultCategoryDataset(), PlotOrientation.VERTICAL, false, false, false);
    }

    /** Rounded border helper */
    private static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int thickness, radius;
        RoundedBorder(Color c, int t, int r) { color = c; thickness = t; radius = r; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }
    }
}
