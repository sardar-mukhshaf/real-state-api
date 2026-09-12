package com.realestate.addresses.domain;

import java.time.Instant;

public record PropertyAddress(
        String id,
        boolean current,
        String propertyId,
        String addressId,
        Instant createdAt,
        Instant updatedAt) {}
