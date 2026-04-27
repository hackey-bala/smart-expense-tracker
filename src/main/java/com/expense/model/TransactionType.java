package com.expense.model;

/**
 * Java 24: sealed interface + enum — no change in structure, but
 * we showcase switch expressions with pattern-matched exhaustiveness.
 */
public enum TransactionType {
    INCOME("Income"),
    EXPENSE("Expense");

    private final String displayName;

    TransactionType(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    /** Java 21+ switch expression — exhaustive, no default needed for sealed types */
    public boolean isDebit() {
        return switch (this) {
            case EXPENSE -> true;
            case INCOME  -> false;
        };
    }

    @Override
    public String toString() { return displayName; }
}
