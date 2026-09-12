package com.realestate.users.infrastructure.persistence;

import com.realestate.users.domain.Tenant;

public final class TenantMapper {
    private TenantMapper() {}

    public static Tenant toDomain(TenantEntity entity) {
        return new Tenant(entity.id);
    }

    public static TenantEntity toEntity(Tenant value) {
        var entity = new TenantEntity();
        entity.id = value.id();
        return entity;
    }
}
