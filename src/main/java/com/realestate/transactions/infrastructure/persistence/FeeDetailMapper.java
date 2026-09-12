package com.realestate.transactions.infrastructure.persistence;

import com.realestate.transactions.domain.FeeDetail;

public final class FeeDetailMapper {
    private FeeDetailMapper() {}

    public static FeeDetail toDomain(FeeDetailEntity entity) {
        return new FeeDetail(entity.id, entity.transactionId, entity.createdAt, entity.updatedAt);
    }

    public static FeeDetailEntity toEntity(FeeDetail value) {
        var entity = new FeeDetailEntity();
        entity.id = value.id();
        entity.transactionId = value.transactionId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
