package com.realestate.transactions.domain;

import java.time.Instant;

public record PaymentDetail(
        String id,
        String status,
        String landlordId,
        String transactionId,
        Instant createdAt,
        Instant updatedAt) {}
