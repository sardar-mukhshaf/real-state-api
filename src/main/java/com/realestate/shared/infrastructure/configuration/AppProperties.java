package com.realestate.shared.infrastructure.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Startup-validated settings owned by the application; infrastructure reads this boundary. */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid Storage storage,
        @NotNull @Valid Bootstrap bootstrap,
        @NotEmpty List<@NotBlank String> corsOrigins,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String environment,
        Map<String, @Valid Limit> rateLimit) {
    public AppProperties {
        corsOrigins = corsOrigins == null ? List.of() : List.copyOf(corsOrigins);
        rateLimit = rateLimit == null ? Map.of() : Map.copyOf(rateLimit);
    }

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotBlank String issuer,
            @NotBlank String audience,
            @Min(60) @Max(3600) long accessSeconds,
            @Min(61) @Max(7776000) long refreshSeconds) {
        @Override
        public String toString() {
            return "Jwt[secret=REDACTED]";
        }
    }

    public record Storage(
            @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]") String bucket,
            @NotBlank String region,
            @NotNull String endpoint,
            @NotNull String publicEndpoint) {
        public Storage {
            for (String value :
                    List.of(
                            Objects.requireNonNullElse(endpoint, ""),
                            Objects.requireNonNullElse(publicEndpoint, ""))) {
                if (!value.isBlank()) {
                    var uri = java.net.URI.create(value);
                    if (!Set.of("http", "https").contains(uri.getScheme())
                            || uri.getHost() == null
                            || uri.getUserInfo() != null
                            || uri.getQuery() != null
                            || uri.getFragment() != null)
                        throw new IllegalArgumentException(
                                "Storage endpoints require an HTTP(S) host without credentials or query");
                }
            }
        }
    }

    public record Bootstrap(@NotNull String email, @NotNull String password) {
        @Override
        public String toString() {
            return "Bootstrap[credentials=REDACTED]";
        }
    }

    public record Limit(@Min(1) @Max(1000000) int limit, @Min(1) @Max(86400) int windowSeconds) {}
}
