package com.realestate.users.domain;

import java.time.Instant;

public record User(
        String id,
        String firstName,
        String lastName,
        String email,
        String passwordHash,
        boolean emailVerified,
        boolean rememberMe,
        boolean isActive,
        String type,
        long securityVersion,
        Instant createdAt,
        Instant updatedAt) {
    @Override
    public String toString() {
        return "User[id=" + id + ", type=" + type + "]";
    }
}
