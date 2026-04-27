package com.expense.ui.panels;

import com.expense.model.FraudAlert;
import com.expense.service.FraudDetectionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FraudPanel extends JPanel {

    private final FraudDetectionService fraudService;

    private JTable alertTable;
    private DefaultTableModel tableModel;
    private JLabel activeCountLabel;
    private JCheckBox showResolvedCheck;

    private final String[] COLUMNS = {
        "ID", "Date", "Transaction", "Alert Type", "Description", "Risk Score", "Status"
    };

    private final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public FraudPanel(FraudDetectionService fraudService) {
        this.fraudService = fraudService;
        initUI();
        loadAlerts();
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(new Color(236, 240, 241));

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setOpaque(false);

        JLabel icon = new JLabel("🚨");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        JLabel titleLabel = new JLabel("Fraud Detection Alerts");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        activeCountLabel = new JLabel("0 active");
        activeCountLabel.setBackground(new Color(231, 76, 60));
        activeCountLabel.setForeground(Color.WHITE);
        activeCountLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        activeCountLabel.setOpaque(true);
        activeCountLabel.setBorder(new EmptyBorder(2, 8, 2, 8));

        headerLeft.add(icon);
        headerLeft.add(titleLabel);
        headerLeft.add(activeCountLabel);
        header.add(headerLeft, BorderLayout.WEST);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        showResolvedCheck = new JCheckBox("Show Resolved");
        showResolvedCheck.setForeground(Color.WHITE);
        showResolvedCheck.setOpaque(false);
        showResolvedCheck.addActionListener(e -> loadAlerts());

        JButton scanBtn    = createButton("🔍 Run Scan", new Color(52, 152, 219));
        JButton resolveBtn = createButton("✓ Mark Resolved", new Color(39, 174, 96));
        JButton refreshBtn = createButton("↻ Refresh", new Color(127, 140, 141));

        controls.add(showResolvedCheck);
        controls.add(scanBtn);
        controls.add(resolveBtn);
        controls.add(refreshBtn);
        header.add(controls, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        alertTable = new JTable(tableModel);
        alertTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        alertTable.setRowHeight(30);
        alertTable.setShowGrid(false);
        alertTable.setIntercellSpacing(new Dimension(0, 0));
        alertTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        alertTable.getTableHeader().setBackground(new Color(44, 62, 80));
        alertTable.getTableHeader().setForeground(Color.WHITE);

        // Hide ID column
        alertTable.getColumnModel().getColumn(0).setMinWidth(0);
        alertTable.getColumnModel().getColumn(0).setMaxWidth(0);

        // Color rows by risk score
        alertTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    String status = (String) t.getValueAt(row, 6);
                    if ("✓ Resolved".equals(status)) {
                        setBackground(new Color(234, 250, 241));
                        setForeground(new Color(127, 140, 141));
                    } else {
                        try {
                            double risk = Double.parseDouble(
                                t.getValueAt(row, 5).toString().replace(" ⚡",""));
                            if (risk >= 7) setBackground(new Color(255, 235, 238));
                            else if (risk >= 4) setBackground(new Color(255, 248, 225));
                            else setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                        } catch (Exception ex) {
                            setBackground(Color.WHITE);
                        }
                        setForeground(Color.BLACK);
                    }
                }
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(alertTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        add(scrollPane, BorderLayout.CENTER);

        // ── Detail Panel ──────────────────────────────────────────────────────
        JTextArea detail = new JTextArea(4, 40);
        detail.setEditable(false);
        detail.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detail.setBackground(new Color(44, 62, 80));
        detail.setForeground(new Color(200, 220, 230));
        detail.setBorder(new EmptyBorder(10, 14, 10, 14));

        alertTable.getSelectionModel().addListSelectionListener(e -> {
            int row = alertTable.getSelectedRow();
            if (row >= 0) {
                String desc = (String) tableModel.getValueAt(row, 4);
                String type = (String) tableModel.getValueAt(row, 3);
                String score = (String) tableModel.getValueAt(row, 5);
                detail.setText(String.format(
                    "Alert Type: %s\nRisk Score: %s\n\nDetails:\n%s", type, score, desc));
            }
        });

        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.setBorder(null);
        detailScroll.setPreferredSize(new Dimension(0, 90));
        add(detailScroll, BorderLayout.SOUTH);

        // ── Events ────────────────────────────────────────────────────────────
        scanBtn.addActionListener(e -> runScan());
        resolveBtn.addActionListener(e -> resolveSelected());
        refreshBtn.addActionListener(e -> loadAlerts());
    }

    private void loadAlerts() {
        tableModel.setRowCount(0);
        List<FraudAlert> alerts = showResolvedCheck.isSelected()
            ? fraudService.getAllAlerts()
            : fraudService.getActiveAlerts();

        long active = alerts.stream().filter(a -> !a.isResolved()).count();
        activeCountLabel.setText(active + " active");

        for (FraudAlert a : alerts) {
            String desc = a.getTransaction().getDescription();
            tableModel.addRow(new Object[]{
                a.getId(),
                a.getDetectedAt().format(DISPLAY_FORMAT),
                desc.length() > 30 ? desc.substring(0, 27) + "..." : desc,
                a.getAlertType(),
                a.getDescription(),
                String.format("%.1f", a.getRiskScore()),
                a.isResolved() ? "✓ Resolved" : "⚠ Active"
            });
        }
    }

    private void resolveSelected() {
        int row = alertTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an alert to resolve.");
            return;
        }
        String status = (String) tableModel.getValueAt(row, 6);
        if ("✓ Resolved".equals(status)) {
            JOptionPane.showMessageDialog(this, "Alert is already resolved.");
            return;
        }
        Long id = (Long) tableModel.getValueAt(row, 0);
        fraudService.resolveAlert(id);
        loadAlerts();
        JOptionPane.showMessageDialog(this, "Alert marked as resolved.",
            "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    private void runScan() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                fraudService.runFullScan();
                return null;
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                loadAlerts();
                JOptionPane.showMessageDialog(FraudPanel.this,
                    "Fraud detection scan complete!",
                    "Scan Complete", JOptionPane.INFORMATION_MESSAGE);
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
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }
}
