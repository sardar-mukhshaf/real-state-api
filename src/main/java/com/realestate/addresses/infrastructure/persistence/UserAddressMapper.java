package com.realestate.addresses.infrastructure.persistence;

import com.realestate.addresses.domain.UserAddress;

public final class UserAddressMapper {
    private UserAddressMapper() {}

    public static UserAddress toDomain(UserAddressEntity entity) {
        return new UserAddress(
                entity.id,
                entity.current,
                entity.userId,
                entity.addressId,
                entity.createdAt,
                entity.updatedAt);
    }

    public static UserAddressEntity toEntity(UserAddress value) {
        var entity = new UserAddressEntity();
        entity.id = value.id();
        entity.current = value.current();
        entity.userId = value.userId();
        entity.addressId = value.addressId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
