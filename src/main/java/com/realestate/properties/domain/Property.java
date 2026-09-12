package com.realestate.properties.domain;

import com.realestate.shared.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record Property(
        String id,
        String name,
        double size,
        BigDecimal price,
        String type,
        String status,
        String landlordId,
        Instant createdAt,
        Instant updatedAt) {
    public Property {
        Rules.id(id);
        Rules.id(landlordId);
        Rules.text(name, "Property name", 3, 255);
        if (!Double.isFinite(size) || size < 1 || size != Math.rint(size))
            throw BusinessException.invalid("Size must be a positive integer");
        price = Rules.money(price, false);
        if (!Set.of("HOUSE", "APARTMENT", "COMMERCIAL").contains(type))
            throw BusinessException.invalid("Invalid property type");
        if (!Set.of("RENT", "SELL", "RENTED", "SOLD").contains(status))
            throw BusinessException.invalid("Invalid property status");
    }
}
