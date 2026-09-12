package com.realestate.contracts.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "\"TenancyContract\"")
public class TenancyContractEntity {
    protected TenancyContractEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"tenant_id\"", nullable = false, columnDefinition = "text")
    String tenantId;

    @Column(name = "\"property_id\"", nullable = false, columnDefinition = "text")
    String propertyId;

    @Column(name = "\"start_date\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant startDate;

    @Column(name = "\"end_date\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant endDate;

    @Column(name = "\"mng_fee_percentage\"", nullable = false, precision = 7, scale = 4)
    BigDecimal mngFeePercentage;

    @Column(name = "\"rent_per_month\"", nullable = false, precision = 19, scale = 2)
    BigDecimal rentPerMonth;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
