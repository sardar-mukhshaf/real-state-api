package com.realestate.auth.infrastructure.web;

import com.realestate.auth.application.AuthService;
import com.realestate.auth.domain.Device;
import com.realestate.shared.infrastructure.web.Responses;
import com.realestate.users.infrastructure.web.UserController.CreateUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
        @Override
        public String toString() {
            return "Login[redacted]";
        }
    }

    public record Refresh(@NotBlank @Size(max = 4096) String refreshToken) {
        @Override
        public String toString() {
            return "Refresh[redacted]";
        }
    }

    public record PasswordChange(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
        @Override
        public String toString() {
            return "PasswordChange[redacted]";
        }
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    private Map<String, Object> publicTokens(Map<String, Object> value) {
        value.remove("_refresh_id");
        return value;
    }

    @PostMapping("/register")
    ResponseEntity<?> register(@Valid @RequestBody CreateUser body, Device device) {
        return Responses.created(
                publicTokens(
                        auth.register(
                                body.firstName(),
                                body.lastName(),
                                body.email(),
                                body.password(),
                                body.type(),
                                device)),
                "User created successfully");
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody Login body, Device device) {
        return Responses.ok(
                publicTokens(auth.login(body.email(), body.password(), device)),
                "User logged in successfully");
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(@Valid @RequestBody Refresh body, Device device) {
        return Responses.ok(
                publicTokens(auth.refresh(body.refreshToken(), device)), "Session refreshed");
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(@AuthenticationPrincipal Jwt jwt) {
        return Responses.ok(
                auth.logout(jwt.getSubject(), jwt.getClaimAsString("family"), false), "Logged out");
    }

    @PostMapping("/logout-all")
    ResponseEntity<?> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        return Responses.ok(
                auth.logout(jwt.getSubject(), jwt.getClaimAsString("family"), true),
                "All sessions revoked");
    }

    @PostMapping("/change-password")
    ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordChange body) {
        return Responses.ok(
                auth.changePassword(jwt.getSubject(), body.currentPassword(), body.newPassword()),
                "Password changed; sign in again");
    }
}
