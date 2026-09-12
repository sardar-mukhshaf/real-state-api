package com.realestate.auth.infrastructure.persistence;

import com.realestate.auth.domain.RefreshSession;

public final class RefreshSessionMapper {
    private RefreshSessionMapper() {}

    public static RefreshSession toDomain(RefreshSessionEntity entity) {
        return new RefreshSession(
                entity.id,
                entity.userId,
                entity.deviceId,
                entity.familyId,
                entity.tokenHash,
                entity.expiresAt,
                entity.revokedAt,
                entity.replacedBy,
                entity.createdAt,
                entity.updatedAt);
    }

    public static RefreshSessionEntity toEntity(RefreshSession value) {
        var entity = new RefreshSessionEntity();
        entity.id = value.id();
        entity.userId = value.userId();
        entity.deviceId = value.deviceId();
        entity.familyId = value.familyId();
        entity.tokenHash = value.tokenHash();
        entity.expiresAt = value.expiresAt();
        entity.revokedAt = value.revokedAt();
        entity.replacedBy = value.replacedBy();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
