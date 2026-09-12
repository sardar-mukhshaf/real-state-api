package com.realestate.addresses.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"UserAddress\"")
public class UserAddressEntity {
    protected UserAddressEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"current\"", nullable = false)
    boolean current;

    @Column(name = "\"user_id\"", nullable = false, columnDefinition = "text")
    String userId;

    @Column(name = "\"address_id\"", nullable = false, columnDefinition = "text")
    String addressId;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
