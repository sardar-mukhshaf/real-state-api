package com.realestate;

import static org.assertj.core.api.Assertions.*;

import com.realestate.auth.infrastructure.security.JwtTokens;
import com.realestate.shared.domain.BusinessException;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtTokensTest {
    private static final String SECRET =
            "test-only-cryptographic-key-material-01234567890123456789";
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    private JwtTokens tokens(Clock value) {
        return new JwtTokens(SECRET, "test-issuer", "test-audience", 900, 86400, value);
    }

    @Test
    void verifiesSignatureTypeAndExpiry() {
        var tokens = tokens(clock);
        String user = UUID.randomUUID().toString();
        String access = tokens.issue(user, "token", "family", "device123", 0, false);
        assertThat(tokens.accessDecoder().decode(access).getSubject()).isEqualTo(user);
        String refresh = tokens.issue(user, "refresh", "family", "device123", 0, true);
        assertThat(tokens.verifyRefresh(refresh).userId()).isEqualTo(user);
        assertThatThrownBy(() -> tokens.accessDecoder().decode(refresh))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> tokens.verifyRefresh(access))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () -> tokens.accessDecoder().decode(access.substring(0, 30) + "tampered"))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(
                        () ->
                                tokens(Clock.offset(clock, Duration.ofHours(2)))
                                        .accessDecoder()
                                        .decode(access))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongIssuerAudienceAndSecret() {
        var other = new JwtTokens(SECRET, "wrong", "test-audience", 900, 86400, clock);
        String token =
                other.issue(UUID.randomUUID().toString(), "id", "family", "device123", 0, false);
        assertThatThrownBy(() -> tokens(clock).accessDecoder().decode(token))
                .isInstanceOf(JwtException.class);
        var audience = new JwtTokens(SECRET, "test-issuer", "wrong", 900, 86400, clock);
        assertThatThrownBy(
                        () ->
                                tokens(clock)
                                        .accessDecoder()
                                        .decode(
                                                audience.issue(
                                                        UUID.randomUUID().toString(),
                                                        "id",
                                                        "family",
                                                        "device123",
                                                        0,
                                                        false)))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> new JwtTokens("short", "issuer", "audience", 900, 86400, clock))
                .isInstanceOf(IllegalStateException.class);
    }
}
