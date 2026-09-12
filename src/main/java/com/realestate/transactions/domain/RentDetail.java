package com.realestate.transactions.domain;

import java.time.Instant;

public record RentDetail(
        String id,
        String tenantId,
        String transactionId,
        String tenancyContractId,
        Instant startDate,
        Instant endDate,
        Instant createdAt,
        Instant updatedAt) {}
