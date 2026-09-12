package com.realestate.properties.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "\"Property\"")
public class PropertyEntity {
    protected PropertyEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"name\"", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "\"size\"", nullable = false)
    double size;

    @Column(name = "\"price\"", nullable = false, precision = 19, scale = 2)
    BigDecimal price;

    @Column(name = "\"type\"", nullable = false, columnDefinition = "text")
    String type;

    @Column(name = "\"status\"", nullable = false, columnDefinition = "text")
    String status;

    @Column(name = "\"landlord_id\"", nullable = false, columnDefinition = "text")
    String landlordId;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
