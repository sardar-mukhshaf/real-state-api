package com.realestate.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"RefreshSession\"")
public class RefreshSessionEntity {
    protected RefreshSessionEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"user_id\"", nullable = false, columnDefinition = "text")
    String userId;

    @Column(name = "\"device_id\"", nullable = false, columnDefinition = "text")
    String deviceId;

    @Column(name = "\"family_id\"", nullable = false, columnDefinition = "text")
    String familyId;

    @Column(name = "\"token_hash\"", nullable = false, columnDefinition = "text")
    String tokenHash;

    @Column(name = "\"expires_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant expiresAt;

    @Column(name = "\"revoked_at\"", nullable = true, columnDefinition = "timestamp(3)")
    Instant revokedAt;

    @Column(name = "\"replaced_by\"", nullable = true, columnDefinition = "text")
    String replacedBy;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
