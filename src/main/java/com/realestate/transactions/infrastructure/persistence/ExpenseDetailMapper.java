package com.realestate.transactions.infrastructure.persistence;

import com.realestate.transactions.domain.ExpenseDetail;

public final class ExpenseDetailMapper {
    private ExpenseDetailMapper() {}

    public static ExpenseDetail toDomain(ExpenseDetailEntity entity) {
        return new ExpenseDetail(
                entity.id, entity.transactionId, entity.createdAt, entity.updatedAt);
    }

    public static ExpenseDetailEntity toEntity(ExpenseDetail value) {
        var entity = new ExpenseDetailEntity();
        entity.id = value.id();
        entity.transactionId = value.transactionId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
