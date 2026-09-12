package com.realestate.addresses.infrastructure.persistence;

import com.realestate.addresses.domain.PropertyAddress;

public final class PropertyAddressMapper {
    private PropertyAddressMapper() {}

    public static PropertyAddress toDomain(PropertyAddressEntity entity) {
        return new PropertyAddress(
                entity.id,
                entity.current,
                entity.propertyId,
                entity.addressId,
                entity.createdAt,
                entity.updatedAt);
    }

    public static PropertyAddressEntity toEntity(PropertyAddress value) {
        var entity = new PropertyAddressEntity();
        entity.id = value.id();
        entity.current = value.current();
        entity.propertyId = value.propertyId();
        entity.addressId = value.addressId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
