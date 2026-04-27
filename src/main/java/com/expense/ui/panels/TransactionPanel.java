package com.expense.ui.panels;

import com.expense.model.*;
import com.expense.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionPanel extends JPanel {

    private final TransactionService transactionService;

    private JTable            table;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterCombo;
    private JTextField        searchField;

    private static final String[] COLUMNS =
        {"ID", "Date", "Description", "Category", "Type", "Amount", "Suspicious"};

    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    public TransactionPanel(TransactionService transactionService) {
        this.transactionService = transactionService;
        initUI();
        loadTransactions();
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setBackground(new Color(236, 240, 241));

        var toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        var addBtn     = createButton("+ Add Transaction", new Color(39, 174, 96));
        var editBtn    = createButton("Edit",              new Color(41, 128, 185));
        var deleteBtn  = createButton("Delete",            new Color(231, 76, 60));
        var refreshBtn = createButton("Refresh",           new Color(127, 140, 141));

        searchField = new JTextField(20);
        filterCombo = new JComboBox<>(new String[]{"All", "Income", "Expense", "Flagged"});

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(new JLabel("Filter:"));
        toolbar.add(filterCombo);
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(refreshBtn);
        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(41, 128, 185));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(174, 214, 241));

        var idCol = table.getColumnModel().getColumn(0);
        idCol.setMinWidth(0); idCol.setMaxWidth(0); idCol.setWidth(0);

        // Java 21: switch expression (stable, no preview needed)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    var type       = (String) t.getValueAt(row, 4);
                    var suspicious = (String) t.getValueAt(row, 6);
                    setBackground(switch (suspicious) {
                        case "Yes" -> new Color(253, 245, 230);
                        default -> switch (type) {
                            case "INCOME" -> new Color(234, 250, 241);
                            default       -> row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250);
                        };
                    });
                }
                return this;
            }
        });

        var scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        add(scrollPane, BorderLayout.CENTER);

        var summaryBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryBar.setOpaque(false);
        var rowCount = new JLabel("0 transactions");
        rowCount.setFont(new Font("SansSerif", Font.PLAIN, 11));
        rowCount.setForeground(new Color(127, 140, 141));
        summaryBar.add(rowCount);
        add(summaryBar, BorderLayout.SOUTH);

        // Java 21: named params in listeners (no unnamed _ preview feature)
        tableModel.addTableModelListener(e ->
            rowCount.setText(tableModel.getRowCount() + " transactions"));

        addBtn.addActionListener(e     -> showAddDialog());
        editBtn.addActionListener(e    -> showEditDialog());
        deleteBtn.addActionListener(e  -> deleteSelected());
        refreshBtn.addActionListener(e -> loadTransactions());
        filterCombo.addActionListener(e -> loadTransactions());
        searchField.addActionListener(e -> loadTransactions());
    }

    private void loadTransactions() {
        tableModel.setRowCount(0);
        var transactions = transactionService.getAllTransactions();
        var filter = (String) filterCombo.getSelectedItem();
        var search = searchField.getText().toLowerCase().strip();

        for (var t : transactions) {
            boolean include = switch (filter) {
                case "Income"  -> t.getType() == TransactionType.INCOME;
                case "Expense" -> t.getType() == TransactionType.EXPENSE;
                case "Flagged" -> t.isFlaggedAsSuspicious();
                default        -> true;
            };

            if (!search.isEmpty()) {
                include = include && t.getDescription().toLowerCase().contains(search);
            }

            if (include) {
                tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getTransactionDate().format(DISPLAY_FORMAT),
                    t.getDescription(),
                    t.getCategory() != null ? t.getCategory().getName() : "-",
                    t.getType().name(),
                    "$%,.2f".formatted(t.getAmount()),
                    t.isFlaggedAsSuspicious() ? "Yes" : "No"
                });
            }
        }
    }

    private void showAddDialog() {
        var dialog = new TransactionDialog(
            SwingUtilities.getWindowAncestor(this),
            "Add Transaction", transactionService, null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            try {
                transactionService.addTransaction(dialog.getTransactionDTO());
                loadTransactions();
                JOptionPane.showMessageDialog(this, "Transaction added!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a transaction to edit."); return; }
        var id = (Long) tableModel.getValueAt(row, 0);
        transactionService.getById(id).ifPresent(t -> {
            var dialog = new TransactionDialog(
                SwingUtilities.getWindowAncestor(this),
                "Edit Transaction", transactionService, t);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                transactionService.updateTransaction(id, dialog.getTransactionDTO());
                loadTransactions();
            }
        });
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a transaction to delete."); return; }
        var desc = (String) tableModel.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete \"" + desc + "\"?", "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            transactionService.deleteTransaction((Long) tableModel.getValueAt(row, 0));
            loadTransactions();
        }
    }

    private JButton createButton(String text, Color color) {
        var btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return btn;
    }
}
