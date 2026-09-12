package com.realestate.contracts.infrastructure.web;

import com.realestate.contracts.application.ContractService;
import com.realestate.shared.application.Changes;
import com.realestate.shared.infrastructure.web.Responses;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contract")
public class ContractController {
    private final ContractService contracts;

    public ContractController(ContractService contracts) {
        this.contracts = contracts;
    }

    @PostMapping("/create")
    ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        var c = contracts.create(new Changes(body));
        return Responses.created(contracts.view(c.id()), "Contract created successfully");
    }

    @GetMapping("/all")
    ResponseEntity<?> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return Responses.ok(
                contracts.views(contracts.list(page, size)), "Contracts fetched successfully");
    }

    @GetMapping("/single/{id}")
    ResponseEntity<?> get(@PathVariable String id) {
        return Responses.ok(contracts.view(id), "Contract fetched successfully");
    }

    @PutMapping("/update/{id}")
    ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        contracts.update(id, new Changes(body));
        return Responses.ok(contracts.view(id), "Contract updated successfully");
    }

    @DeleteMapping("/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String id) {
        return Responses.ok(contracts.delete(id), "Contract deleted successfully");
    }
}
