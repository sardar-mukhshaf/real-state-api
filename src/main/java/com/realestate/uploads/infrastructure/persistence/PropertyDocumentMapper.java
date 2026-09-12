package com.realestate.uploads.infrastructure.persistence;

import com.realestate.uploads.domain.PropertyDocument;

public final class PropertyDocumentMapper {
    private PropertyDocumentMapper() {}

    public static PropertyDocument toDomain(PropertyDocumentEntity entity) {
        return new PropertyDocument(
                entity.id,
                entity.propertyId,
                entity.fileId,
                entity.documentType,
                entity.description,
                entity.notes,
                entity.createdAt,
                entity.updatedAt);
    }

    public static PropertyDocumentEntity toEntity(PropertyDocument value) {
        var entity = new PropertyDocumentEntity();
        entity.id = value.id();
        entity.propertyId = value.propertyId();
        entity.fileId = value.fileId();
        entity.documentType = value.documentType();
        entity.description = value.description();
        entity.notes = value.notes();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
