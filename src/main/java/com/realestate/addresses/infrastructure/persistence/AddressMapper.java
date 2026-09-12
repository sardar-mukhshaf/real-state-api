package com.realestate.addresses.infrastructure.persistence;

import com.realestate.addresses.domain.Address;

public final class AddressMapper {
    private AddressMapper() {}

    public static Address toDomain(AddressEntity entity) {
        return new Address(
                entity.id,
                entity.houseNumber,
                entity.buildingName,
                entity.street,
                entity.town,
                entity.city,
                entity.postalCode,
                entity.description,
                entity.notes,
                entity.createdAt,
                entity.updatedAt);
    }

    public static AddressEntity toEntity(Address value) {
        var entity = new AddressEntity();
        entity.id = value.id();
        entity.houseNumber = value.houseNumber();
        entity.buildingName = value.buildingName();
        entity.street = value.street();
        entity.town = value.town();
        entity.city = value.city();
        entity.postalCode = value.postalCode();
        entity.description = value.description();
        entity.notes = value.notes();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
