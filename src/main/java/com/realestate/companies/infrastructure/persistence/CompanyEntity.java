package com.realestate.companies.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"CompanyDetails\"")
public class CompanyEntity {
    protected CompanyEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"name\"", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "\"vat_reg_no\"", nullable = false, columnDefinition = "text")
    String vatRegNo;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
