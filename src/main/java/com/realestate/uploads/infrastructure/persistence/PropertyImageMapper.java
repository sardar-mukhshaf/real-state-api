package com.realestate.uploads.infrastructure.persistence;

import com.realestate.uploads.domain.PropertyImage;

public final class PropertyImageMapper {
    private PropertyImageMapper() {}

    public static PropertyImage toDomain(PropertyImageEntity entity) {
        return new PropertyImage(
                entity.id,
                entity.propertyId,
                entity.fileId,
                entity.imageType,
                entity.description,
                entity.notes,
                entity.createdAt,
                entity.updatedAt);
    }

    public static PropertyImageEntity toEntity(PropertyImage value) {
        var entity = new PropertyImageEntity();
        entity.id = value.id();
        entity.propertyId = value.propertyId();
        entity.fileId = value.fileId();
        entity.imageType = value.imageType();
        entity.description = value.description();
        entity.notes = value.notes();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
