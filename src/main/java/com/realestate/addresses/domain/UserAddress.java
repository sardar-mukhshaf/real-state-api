package com.realestate.addresses.domain;

import java.time.Instant;

public record UserAddress(
        String id,
        boolean current,
        String userId,
        String addressId,
        Instant createdAt,
        Instant updatedAt) {}
