package com.realestate.addresses.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"Address\"")
public class AddressEntity {
    protected AddressEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"house_number\"", nullable = false, columnDefinition = "text")
    String houseNumber;

    @Column(name = "\"building_name\"", nullable = false, columnDefinition = "text")
    String buildingName;

    @Column(name = "\"street\"", nullable = false, columnDefinition = "text")
    String street;

    @Column(name = "\"town\"", nullable = false, columnDefinition = "text")
    String town;

    @Column(name = "\"city\"", nullable = false, columnDefinition = "text")
    String city;

    @Column(name = "\"postal_code\"", nullable = false, columnDefinition = "text")
    String postalCode;

    @Column(name = "\"description\"", nullable = false, columnDefinition = "text")
    String description;

    @Column(name = "\"notes\"", nullable = false, columnDefinition = "text")
    String notes;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
