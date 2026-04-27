package com.expense.ui.panels;

import com.expense.dto.TransactionDTO;
import com.expense.model.*;
import com.expense.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionDialog extends JDialog {

    private final TransactionService transactionService;
    private final Transaction        existing;

    private JTextField   descField, amountField, notesField, dateField;
    private JComboBox<String>   typeCombo;
    private JComboBox<Category> categoryCombo;

    private boolean        confirmed    = false;
    private TransactionDTO confirmedDTO = null;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TransactionDialog(Window parent, String title,
                              TransactionService service, Transaction existing) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.transactionService = service;
        this.existing           = existing;
        buildUI();
        if (existing != null) populateFields();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void buildUI() {
        var main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(20, 24, 16, 24));

        var form = new JPanel(new GridBagLayout());
        var gc   = new GridBagConstraints();
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;

        descField     = new JTextField(25);
        amountField   = new JTextField(25);
        notesField    = new JTextField(25);
        dateField     = new JTextField(LocalDateTime.now().format(DATE_FMT), 25);
        typeCombo     = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});
        categoryCombo = new JComboBox<>();

        typeCombo.addActionListener(e -> updateCategories());
        updateCategories();

        addRow(form, gc, 0, "Description *",           descField);
        addRow(form, gc, 1, "Amount ($) *",             amountField);
        addRow(form, gc, 2, "Type *",                   typeCombo);
        addRow(form, gc, 3, "Category",                 categoryCombo);
        addRow(form, gc, 4, "Date (yyyy-MM-dd HH:mm)", dateField);
        addRow(form, gc, 5, "Notes",                    notesField);

        main.add(form, BorderLayout.CENTER);

        var buttons   = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        var saveBtn   = new JButton("Save");
        var cancelBtn = new JButton("Cancel");

        saveBtn.setBackground(new Color(39, 174, 96));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        saveBtn.addActionListener(e   -> handleSave());
        cancelBtn.addActionListener(e -> dispose());

        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        main.add(buttons, BorderLayout.SOUTH);
        setContentPane(main);
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int row,
                         String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        var lbl = new JLabel(label + ":");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 1;
        panel.add(field, gc);
    }

    private void updateCategories() {
        categoryCombo.removeAllItems();
        categoryCombo.addItem(null);
        var selectedType = (String) typeCombo.getSelectedItem();
        var type = "INCOME".equals(selectedType)
            ? TransactionType.INCOME : TransactionType.EXPENSE;
        transactionService.getCategoriesByType(type).forEach(categoryCombo::addItem);
    }

    private void populateFields() {
        descField.setText(existing.getDescription());
        amountField.setText(existing.getAmount().toPlainString());
        typeCombo.setSelectedItem(existing.getType().name());
        updateCategories();
        if (existing.getCategory() != null) {
            for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                var c = categoryCombo.getItemAt(i);
                if (c != null && c.getId().equals(existing.getCategory().getId())) {
                    categoryCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        dateField.setText(existing.getTransactionDate().format(DATE_FMT));
        notesField.setText(existing.getNotes() != null ? existing.getNotes() : "");
    }

    private void handleSave() {
        if (descField.getText().isBlank()) { err("Description is required."); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getText().strip());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {   // named — Java 21 compatible
            err("Enter a valid positive amount."); return;
        }

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(dateField.getText().strip(), DATE_FMT);
        } catch (Exception ex) {
            err("Date format must be: yyyy-MM-dd HH:mm"); return;
        }

        var selectedCat = (Category) categoryCombo.getSelectedItem();

        // Java 21: record constructor (records stable since Java 16)
        confirmedDTO = new TransactionDTO(
            descField.getText().strip(),
            amount,
            TransactionType.valueOf((String) typeCombo.getSelectedItem()),
            selectedCat != null ? selectedCat.getId() : null,
            date,
            notesField.getText().strip()
        );

        confirmed = true;
        dispose();
    }

    public TransactionDTO getTransactionDTO() { return confirmedDTO; }
    public boolean isConfirmed()              { return confirmed; }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
}
