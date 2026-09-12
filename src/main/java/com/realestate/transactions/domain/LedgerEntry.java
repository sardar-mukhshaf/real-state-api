package com.realestate.transactions.domain;

import com.realestate.shared.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record LedgerEntry(
        String id,
        BigDecimal amount,
        boolean isVAT,
        String type,
        String description,
        Instant transactionDate,
        int transactionMonth,
        String propertyId,
        String sourceRentId,
        Instant createdAt,
        Instant updatedAt) {
    public LedgerEntry {
        Rules.id(id);
        Rules.id(propertyId);
        amount = Rules.money(amount, true);
        if (!Set.of("RENT", "EXPENSE", "MANAGEMENT", "PAYMENT").contains(type))
            throw BusinessException.invalid("Invalid transaction type");
        Rules.text(description, "Description", 0, 2000);
        if (transactionDate == null || transactionMonth < 1 || transactionMonth > 12)
            throw BusinessException.invalid("Transaction date and month (1–12) are required");
        if (sourceRentId != null) Rules.id(sourceRentId);
    }

    public void editable() {
        if (sourceRentId != null)
            throw BusinessException.conflict(
                    "Edit the source rent to change a generated transaction");
    }
}
