package com.realestate.uploads.domain;

import java.time.Instant;

public record PropertyImage(
        String id,
        String propertyId,
        String fileId,
        String imageType,
        String description,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
