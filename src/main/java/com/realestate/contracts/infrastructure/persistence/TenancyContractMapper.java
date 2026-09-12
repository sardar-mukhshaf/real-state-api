package com.realestate.contracts.infrastructure.persistence;

import com.realestate.contracts.domain.TenancyContract;

public final class TenancyContractMapper {
    private TenancyContractMapper() {}

    public static TenancyContract toDomain(TenancyContractEntity entity) {
        return new TenancyContract(
                entity.id,
                entity.tenantId,
                entity.propertyId,
                entity.startDate,
                entity.endDate,
                entity.mngFeePercentage,
                entity.rentPerMonth,
                entity.createdAt,
                entity.updatedAt);
    }

    public static TenancyContractEntity toEntity(TenancyContract value) {
        var entity = new TenancyContractEntity();
        entity.id = value.id();
        entity.tenantId = value.tenantId();
        entity.propertyId = value.propertyId();
        entity.startDate = value.startDate();
        entity.endDate = value.endDate();
        entity.mngFeePercentage = value.mngFeePercentage();
        entity.rentPerMonth = value.rentPerMonth();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
