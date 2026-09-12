package com.realestate.transactions.infrastructure.persistence;

import com.realestate.transactions.domain.PaymentDetail;

public final class PaymentDetailMapper {
    private PaymentDetailMapper() {}

    public static PaymentDetail toDomain(PaymentDetailEntity entity) {
        return new PaymentDetail(
                entity.id,
                entity.status,
                entity.landlordId,
                entity.transactionId,
                entity.createdAt,
                entity.updatedAt);
    }

    public static PaymentDetailEntity toEntity(PaymentDetail value) {
        var entity = new PaymentDetailEntity();
        entity.id = value.id();
        entity.status = value.status();
        entity.landlordId = value.landlordId();
        entity.transactionId = value.transactionId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
