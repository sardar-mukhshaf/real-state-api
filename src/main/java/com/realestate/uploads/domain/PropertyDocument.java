package com.realestate.uploads.domain;

import java.time.Instant;

public record PropertyDocument(
        String id,
        String propertyId,
        String fileId,
        String documentType,
        String description,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
