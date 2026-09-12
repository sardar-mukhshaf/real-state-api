package com.realestate.users.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"User\"")
public class UserEntity {
    protected UserEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"first_name\"", nullable = false, columnDefinition = "text")
    String firstName;

    @Column(name = "\"last_name\"", nullable = false, columnDefinition = "text")
    String lastName;

    @Column(name = "\"email\"", nullable = false, columnDefinition = "text")
    String email;

    @Column(name = "\"password\"", nullable = false, columnDefinition = "text")
    String passwordHash;

    @Column(name = "\"email_verified\"", nullable = false)
    boolean emailVerified;

    @Column(name = "\"remember_me\"", nullable = false)
    boolean rememberMe;

    @Column(name = "\"is_active\"", nullable = false)
    boolean isActive;

    @Column(name = "\"type\"", nullable = false, columnDefinition = "text")
    String type;

    @Column(name = "\"security_version\"", nullable = false)
    long securityVersion;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
