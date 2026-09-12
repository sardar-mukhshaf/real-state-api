package com.realestate.addresses.infrastructure.web;

import com.realestate.addresses.application.AddressService;
import com.realestate.shared.application.*;
import com.realestate.shared.infrastructure.web.Responses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/address")
public class AddressController {
    public record CreateAddress(
            @NotBlank @Size(max = 100) String houseNumber,
            @Size(max = 255) String buildingName,
            @NotBlank @Size(max = 255) String street,
            @NotBlank @Size(max = 255) String town,
            @NotBlank @Size(max = 255) String city,
            @NotBlank @Size(max = 32) String postalCode,
            @Size(max = 2000) String description,
            @Size(max = 2000) String notes) {
        Changes command() {
            return new Changes(
                    Data.map(
                            "house_number",
                            houseNumber,
                            "building_name",
                            buildingName,
                            "street",
                            street,
                            "town",
                            town,
                            "city",
                            city,
                            "postal_code",
                            postalCode,
                            "description",
                            description,
                            "notes",
                            notes));
        }
    }

    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @PostMapping("/create")
    ResponseEntity<?> create(@Valid @RequestBody CreateAddress data) {
        return Responses.created(addresses.create(data.command()), "Address created successfully");
    }

    @GetMapping("/all")
    ResponseEntity<?> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return Responses.ok(addresses.list(page, size), "Addresses fetched successfully");
    }

    @PutMapping("/update/{id}")
    ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return Responses.ok(
                addresses.update(id, new Changes(body)), "Address updated successfully");
    }

    @DeleteMapping("/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String id) {
        return Responses.ok(addresses.delete(id), "Address deleted successfully");
    }
}
