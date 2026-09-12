package com.realestate.transactions.domain;

import java.time.Instant;

public record FeeDetail(String id, String transactionId, Instant createdAt, Instant updatedAt) {}
