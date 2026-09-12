package com.realestate.users.infrastructure.web;

import com.realestate.shared.application.Changes;
import com.realestate.shared.infrastructure.web.Responses;
import com.realestate.users.application.UserService;
import com.realestate.users.application.UserViews;
import com.realestate.users.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    public record CreateUser(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Email @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            String type) {
        @Override
        public String toString() {
            return "CreateUser[redacted]";
        }
    }

    private final UserService users;
    private final UserViews views;

    public UserController(UserService users, UserViews views) {
        this.users = users;
        this.views = views;
    }

    @PostMapping("/{role:landlord|tenant}/create")
    ResponseEntity<?> create(@PathVariable String role, @Valid @RequestBody CreateUser request) {
        return Responses.created(
                UserService.view(
                        users.create(
                                request.firstName(),
                                request.lastName(),
                                request.email(),
                                request.password(),
                                role(role))),
                "User created successfully");
    }

    @GetMapping("/{role:landlord|tenant}/all")
    ResponseEntity<?> list(
            @PathVariable String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return Responses.ok(
                views.views(users.list(role(role), page, size)), "Users fetched successfully");
    }

    @GetMapping("/single/{id}")
    ResponseEntity<?> single(@PathVariable String id) {
        return Responses.ok(views.one(id), "User fetched successfully");
    }

    @PutMapping("/{role:landlord|tenant}/update/{id}")
    ResponseEntity<?> update(
            @PathVariable String role,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return Responses.ok(
                UserService.view(users.update(id, new Changes(body), role(role))),
                "User updated successfully");
    }

    @DeleteMapping("/{role:landlord|tenant}/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String role, @PathVariable String id) {
        return Responses.ok(users.delete(id, role(role)), "User deleted successfully");
    }

    private UserRole role(String role) {
        return UserRole.valueOf(role.toUpperCase(Locale.ROOT));
    }
}
