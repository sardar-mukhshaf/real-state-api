package com.realestate.uploads.infrastructure.persistence;

import com.realestate.uploads.domain.StoredFile;

public final class StoredFileMapper {
    private StoredFileMapper() {}

    public static StoredFile toDomain(StoredFileEntity entity) {
        return new StoredFile(
                entity.id,
                entity.name,
                entity.path,
                entity.type,
                entity.size,
                entity.createdAt,
                entity.updatedAt);
    }

    public static StoredFileEntity toEntity(StoredFile value) {
        var entity = new StoredFileEntity();
        entity.id = value.id();
        entity.name = value.name();
        entity.path = value.path();
        entity.type = value.type();
        entity.size = value.size();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
