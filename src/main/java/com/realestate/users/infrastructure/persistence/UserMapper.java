package com.realestate.users.infrastructure.persistence;

import com.realestate.users.domain.User;

public final class UserMapper {
    private UserMapper() {}

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.id,
                entity.firstName,
                entity.lastName,
                entity.email,
                entity.passwordHash,
                entity.emailVerified,
                entity.rememberMe,
                entity.isActive,
                entity.type,
                entity.securityVersion,
                entity.createdAt,
                entity.updatedAt);
    }

    public static UserEntity toEntity(User value) {
        var entity = new UserEntity();
        entity.id = value.id();
        entity.firstName = value.firstName();
        entity.lastName = value.lastName();
        entity.email = value.email();
        entity.passwordHash = value.passwordHash();
        entity.emailVerified = value.emailVerified();
        entity.rememberMe = value.rememberMe();
        entity.isActive = value.isActive();
        entity.type = value.type();
        entity.securityVersion = value.securityVersion();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
