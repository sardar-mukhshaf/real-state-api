package com.realestate.auth.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.realestate.auth.application.TokenService;
import com.realestate.shared.domain.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtTokens implements TokenService {
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder accessDecoder;
    private final NimbusJwtDecoder refreshDecoder;
    private final String issuer;
    private final String audience;
    private final long accessSeconds;
    private final long refreshSeconds;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtTokens(
            com.realestate.shared.infrastructure.configuration.AppProperties properties,
            Clock clock) {
        this(
                properties.jwt().secret(),
                properties.jwt().issuer(),
                properties.jwt().audience(),
                properties.jwt().accessSeconds(),
                properties.jwt().refreshSeconds(),
                clock);
    }

    public JwtTokens(
            String secret,
            String issuer,
            String audience,
            long accessSeconds,
            long refreshSeconds,
            Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32 || secret.startsWith("change-me"))
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 bytes of random secret material");
        if (accessSeconds < 60
                || accessSeconds > 3600
                || refreshSeconds <= accessSeconds
                || refreshSeconds > 7776000)
            throw new IllegalStateException("Invalid JWT lifetimes");
        this.issuer = issuer;
        this.audience = audience;
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
        this.clock = clock;
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        accessDecoder = decoder(key, false);
        refreshDecoder = decoder(key, true);
    }

    private NimbusJwtDecoder decoder(SecretKeySpec key, boolean refresh) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> required =
                jwt -> {
                    boolean valid =
                            jwt.getSubject() != null
                                    && jwt.getSubject().matches("[0-9a-fA-F-]{36}")
                                    && jwt.getAudience().contains(audience)
                                    && jwt.getExpiresAt() != null
                                    && jwt.getIssuedAt() != null
                                    && jwt.getNotBefore() != null
                                    && jwt.getId() != null
                                    && jwt.getClaimAsString("family") != null
                                    && jwt.getClaimAsString("device") != null
                                    && jwt.getClaim("version") instanceof Number
                                    && (refresh ? "refresh" : "access")
                                            .equals(jwt.getClaimAsString("token_use"))
                                    && jwt.getIssuedAt().isBefore(clock.instant().plusSeconds(31));
                    return valid
                            ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                };
        var timestamp = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamp.setClock(clock);
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        timestamp, new JwtIssuerValidator(issuer), required));
        return decoder;
    }

    public JwtDecoder accessDecoder() {
        return accessDecoder;
    }

    public String issue(
            String userId,
            String tokenId,
            String familyId,
            String deviceId,
            long version,
            boolean refresh) {
        var now = clock.instant();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .audience(List.of(audience))
                        .subject(userId)
                        .id(tokenId)
                        .issuedAt(now)
                        .notBefore(now)
                        .expiresAt(now.plusSeconds(refresh ? refreshSeconds : accessSeconds))
                        .claim("token_use", refresh ? "refresh" : "access")
                        .claim("family", familyId)
                        .claim("device", deviceId)
                        .claim("version", version)
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims))
                .getTokenValue();
    }

    public Claims verifyRefresh(String raw) {
        try {
            var jwt = refreshDecoder.decode(raw);
            return new Claims(
                    jwt.getSubject(),
                    jwt.getId(),
                    jwt.getClaimAsString("family"),
                    jwt.getClaimAsString("device"),
                    ((Number) jwt.getClaim("version")).longValue(),
                    jwt.getExpiresAt());
        } catch (RuntimeException ex) {
            throw BusinessException.unauthorized();
        }
    }

    public long accessSeconds() {
        return accessSeconds;
    }

    public long refreshSeconds() {
        return refreshSeconds;
    }
}
