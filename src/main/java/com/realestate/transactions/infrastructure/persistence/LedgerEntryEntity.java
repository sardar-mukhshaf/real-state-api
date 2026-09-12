package com.realestate.transactions.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "\"Transaction\"")
public class LedgerEntryEntity {
    protected LedgerEntryEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"amount\"", nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(name = "\"is_VAT\"", nullable = false)
    boolean isVAT;

    @Column(name = "\"type\"", nullable = false, columnDefinition = "text")
    String type;

    @Column(name = "\"description\"", nullable = false, columnDefinition = "text")
    String description;

    @Column(name = "\"transaction_date\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant transactionDate;

    @Column(name = "\"transaction_month\"", nullable = false)
    int transactionMonth;

    @Column(name = "\"property_id\"", nullable = false, columnDefinition = "text")
    String propertyId;

    @Column(name = "\"source_rent_id\"", nullable = true, columnDefinition = "text")
    String sourceRentId;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
