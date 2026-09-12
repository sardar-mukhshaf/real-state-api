package com.realestate.addresses.domain;

import com.realestate.shared.domain.Rules;
import java.time.Instant;

public record Address(
        String id,
        String houseNumber,
        String buildingName,
        String street,
        String town,
        String city,
        String postalCode,
        String description,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
    public Address {
        Rules.id(id);
        Rules.text(houseNumber, "House number", 1, 100);
        Rules.text(buildingName, "Building name", 0, 255);
        Rules.text(street, "Street", 1, 255);
        Rules.text(town, "Town", 1, 255);
        Rules.text(city, "City", 1, 255);
        Rules.text(postalCode, "Postal code", 1, 32);
        Rules.text(description, "Description", 0, 2000);
        Rules.text(notes, "Notes", 0, 2000);
    }

    public String searchText() {
        return String.join(" ", houseNumber, buildingName, street, town, city, postalCode)
                .toLowerCase(java.util.Locale.ROOT);
    }
}
