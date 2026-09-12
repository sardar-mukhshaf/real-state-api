package com.realestate.transactions.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"RentTransaction\"")
public class RentDetailEntity {
    protected RentDetailEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"tenant_id\"", nullable = false, columnDefinition = "text")
    String tenantId;

    @Column(name = "\"transaction_id\"", nullable = false, columnDefinition = "text")
    String transactionId;

    @Column(name = "\"tenancy_contract_id\"", nullable = false, columnDefinition = "text")
    String tenancyContractId;

    @Column(name = "\"start_date\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant startDate;

    @Column(name = "\"end_date\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant endDate;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
