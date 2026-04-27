package com.expense.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIX: EAGER — UI accesses transaction details outside session
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private String alertType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private double riskScore;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
