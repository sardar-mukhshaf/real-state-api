package com.realestate.transactions.infrastructure.persistence;

import com.realestate.transactions.domain.RentDetail;

public final class RentDetailMapper {
    private RentDetailMapper() {}

    public static RentDetail toDomain(RentDetailEntity entity) {
        return new RentDetail(
                entity.id,
                entity.tenantId,
                entity.transactionId,
                entity.tenancyContractId,
                entity.startDate,
                entity.endDate,
                entity.createdAt,
                entity.updatedAt);
    }

    public static RentDetailEntity toEntity(RentDetail value) {
        var entity = new RentDetailEntity();
        entity.id = value.id();
        entity.tenantId = value.tenantId();
        entity.transactionId = value.transactionId();
        entity.tenancyContractId = value.tenancyContractId();
        entity.startDate = value.startDate();
        entity.endDate = value.endDate();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
