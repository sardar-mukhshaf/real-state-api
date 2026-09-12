package com.realestate.transactions.infrastructure.persistence;

import com.realestate.transactions.domain.LedgerEntry;

public final class LedgerEntryMapper {
    private LedgerEntryMapper() {}

    public static LedgerEntry toDomain(LedgerEntryEntity entity) {
        return new LedgerEntry(
                entity.id,
                entity.amount,
                entity.isVAT,
                entity.type,
                entity.description,
                entity.transactionDate,
                entity.transactionMonth,
                entity.propertyId,
                entity.sourceRentId,
                entity.createdAt,
                entity.updatedAt);
    }

    public static LedgerEntryEntity toEntity(LedgerEntry value) {
        var entity = new LedgerEntryEntity();
        entity.id = value.id();
        entity.amount = value.amount();
        entity.isVAT = value.isVAT();
        entity.type = value.type();
        entity.description = value.description();
        entity.transactionDate = value.transactionDate();
        entity.transactionMonth = value.transactionMonth();
        entity.propertyId = value.propertyId();
        entity.sourceRentId = value.sourceRentId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
