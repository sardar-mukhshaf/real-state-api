package com.realestate.reports.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.reports.application.ReportService;
import com.realestate.shared.infrastructure.web.Responses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    public record Summary(
            @NotBlank String propertyId,
            @JsonProperty("startDate") String startDate,
            @JsonProperty("endDate") String endDate,
            @Min(1900) @Max(9998) Integer year) {}

    public record Monthly(
            @NotBlank String propertyId,
            @NotNull @Min(1) @Max(12) Integer month,
            @Min(1900) @Max(9998) Integer year) {}

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @PostMapping("/summary")
    ResponseEntity<?> summary(@Valid @RequestBody Summary body) {
        return Responses.ok(
                reports.summary(body.propertyId(), body.startDate(), body.endDate(), body.year()),
                "Report generated successfully");
    }

    @PostMapping("/monthly")
    ResponseEntity<?> monthly(@Valid @RequestBody Monthly body) {
        return Responses.ok(
                reports.monthly(body.propertyId(), body.month(), body.year()),
                "Monthly report generated successfully");
    }
}
