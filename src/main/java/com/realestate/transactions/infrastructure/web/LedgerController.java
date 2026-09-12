package com.realestate.transactions.infrastructure.web;

import com.realestate.shared.application.Changes;
import com.realestate.shared.domain.BusinessException;
import com.realestate.shared.infrastructure.web.Responses;
import com.realestate.transactions.application.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LedgerController {
    private final LedgerService ledger;
    private final LedgerViews views;

    public LedgerController(LedgerService ledger, LedgerViews views) {
        this.ledger = ledger;
        this.views = views;
    }

    @PostMapping("/{feature:transaction|rent|expense|management-fee|landlord-payment}/create")
    ResponseEntity<?> create(
            @PathVariable String feature,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var kind = kind(feature);
        var entry = ledger.create(new Changes(body), kind, jwt.getSubject(), key);
        return Responses.created(views.one(entry.id(), kind), "Transaction created successfully");
    }

    @PutMapping("/{feature:transaction|rent|expense|management-fee|landlord-payment}/update/{id}")
    ResponseEntity<?> update(
            @PathVariable String feature,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        var entry = ledger.update(id, new Changes(body), kind(feature));
        return Responses.ok(
                views.one(entry.id(), kind(feature)), "Transaction updated successfully");
    }

    @PutMapping("/landlord-payment/update/payment-status/{id}")
    ResponseEntity<?> paymentStatus(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        var patch = new Changes(body);
        patch.only("status");
        var value = ledger.paymentStatus(id, patch.string("status", ""));
        return Responses.ok(
                views.one(value.id(), LedgerService.Kind.PAYMENT),
                "Landlord payment transaction updated successfully");
    }

    @GetMapping("/transaction/single/{id}")
    ResponseEntity<?> single(@PathVariable String id) {
        ledger.get(id);
        return Responses.ok(
                views.one(id, LedgerService.Kind.BASE), "Transaction fetched successfully");
    }

    @DeleteMapping("/transaction/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String id) {
        return Responses.ok(ledger.delete(id), "Transaction deleted successfully");
    }

    @GetMapping({"/transaction/all", "/transaction/all/{propertyId}"})
    ResponseEntity<?> all(
            @PathVariable(required = false) String propertyId,
            @RequestParam(required = false) String propertyName,
            @RequestParam(required = false) List<String> types,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "transaction_date") String sort,
            @RequestParam(defaultValue = "true") boolean descending) {
        var result = views.search(propertyId, propertyName, types, page, size, sort, descending);
        var body =
                new LinkedHashMap<>(
                        Responses.ok(
                                        views.views(result.items(), LedgerService.Kind.BASE),
                                        "Transactions fetched successfully")
                                .getBody());
        body.put(
                "pagination",
                Responses.map(
                        "page",
                        page,
                        "size",
                        size,
                        "totalElements",
                        result.totalElements(),
                        "totalPages",
                        result.totalPages()));
        return ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(result.totalElements()))
                .body(body);
    }

    private LedgerService.Kind kind(String feature) {
        return switch (feature) {
            case "transaction" -> LedgerService.Kind.BASE;
            case "rent" -> LedgerService.Kind.RENT;
            case "expense" -> LedgerService.Kind.EXPENSE;
            case "management-fee" -> LedgerService.Kind.MANAGEMENT;
            case "landlord-payment" -> LedgerService.Kind.PAYMENT;
            default -> throw BusinessException.invalid("Unknown transaction feature");
        };
    }
}
