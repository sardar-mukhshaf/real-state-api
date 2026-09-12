package com.realestate.companies.domain;

import com.realestate.shared.domain.Rules;
import java.time.Instant;

public record Company(
        String id, String name, String vatRegNo, Instant createdAt, Instant updatedAt) {
    public Company {
        Rules.id(id);
        Rules.text(name, "Company name", 1, 255);
        Rules.text(vatRegNo, "VAT registration number", 1, 100);
    }
}
