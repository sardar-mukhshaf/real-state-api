package com.realestate.properties.infrastructure.persistence;

import com.realestate.properties.domain.Property;

public final class PropertyMapper {
    private PropertyMapper() {}

    public static Property toDomain(PropertyEntity entity) {
        return new Property(
                entity.id,
                entity.name,
                entity.size,
                entity.price,
                entity.type,
                entity.status,
                entity.landlordId,
                entity.createdAt,
                entity.updatedAt);
    }

    public static PropertyEntity toEntity(Property value) {
        var entity = new PropertyEntity();
        entity.id = value.id();
        entity.name = value.name();
        entity.size = value.size();
        entity.price = value.price();
        entity.type = value.type();
        entity.status = value.status();
        entity.landlordId = value.landlordId();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
