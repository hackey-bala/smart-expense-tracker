package com.expense.dto;

import com.expense.model.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Java 24: plain record replaces Lombok @Data/@Builder boilerplate.
 * Records are final, immutable, and auto-generate equals/hashCode/toString/accessors.
 */
public record TransactionDTO(
    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Type is required")
    TransactionType type,

    Long categoryId,
    LocalDateTime transactionDate,
    String notes
) {
    /** Compact canonical constructor — normalises whitespace */
    public TransactionDTO {
        description = description == null ? null : description.strip();
        notes       = notes       == null ? null : notes.strip();
        if (transactionDate == null) transactionDate = LocalDateTime.now();
    }
}
