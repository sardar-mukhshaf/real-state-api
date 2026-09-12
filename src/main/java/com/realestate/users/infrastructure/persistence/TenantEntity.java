package com.realestate.users.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "\"Tenant\"")
public class TenantEntity {
    protected TenantEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;
}
