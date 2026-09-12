package com.realestate.auth.domain;

import java.time.Instant;

public record RefreshSession(
        String id,
        String userId,
        String deviceId,
        String familyId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        String replacedBy,
        Instant createdAt,
        Instant updatedAt) {}
