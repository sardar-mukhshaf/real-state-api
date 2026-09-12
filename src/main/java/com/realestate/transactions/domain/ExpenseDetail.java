package com.realestate.transactions.domain;

import java.time.Instant;

public record ExpenseDetail(
        String id, String transactionId, Instant createdAt, Instant updatedAt) {}
