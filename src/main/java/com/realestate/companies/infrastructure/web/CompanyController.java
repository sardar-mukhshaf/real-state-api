package com.realestate.companies.infrastructure.web;

import com.realestate.companies.application.CompanyService;
import com.realestate.shared.application.Changes;
import com.realestate.shared.infrastructure.web.Responses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
    public record CreateCompany(
            @NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 100) String vatRegNo) {}

    private final CompanyService companies;

    public CompanyController(CompanyService companies) {
        this.companies = companies;
    }

    @PostMapping("/create")
    ResponseEntity<?> create(@Valid @RequestBody CreateCompany body) {
        return Responses.created(
                companies.create(body.name(), body.vatRegNo()), "Company created successfully");
    }

    @GetMapping("/all")
    ResponseEntity<?> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return Responses.ok(companies.list(page, size), "Companies fetched successfully");
    }

    @GetMapping("/single/{id}")
    ResponseEntity<?> get(@PathVariable String id) {
        return Responses.ok(companies.get(id), "Company fetched successfully");
    }

    @PutMapping("/update/{id}")
    ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return Responses.ok(
                companies.update(id, new Changes(body)), "Company updated successfully");
    }

    @DeleteMapping("/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String id) {
        return Responses.ok(companies.delete(id), "Company deleted successfully");
    }
}
