package com.realestate.transactions.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"LandlordPaymentTransaction\"")
public class PaymentDetailEntity {
    protected PaymentDetailEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"status\"", nullable = false, columnDefinition = "text")
    String status;

    @Column(name = "\"landlord_id\"", nullable = false, columnDefinition = "text")
    String landlordId;

    @Column(name = "\"transaction_id\"", nullable = false, columnDefinition = "text")
    String transactionId;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
