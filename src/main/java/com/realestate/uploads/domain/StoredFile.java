package com.realestate.uploads.domain;

import java.time.Instant;

public record StoredFile(
        String id,
        String name,
        String path,
        String type,
        int size,
        Instant createdAt,
        Instant updatedAt) {}
