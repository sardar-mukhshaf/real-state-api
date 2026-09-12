package com.realestate.auth.infrastructure.security;

import com.realestate.auth.application.RefreshSessionRepository;
import com.realestate.users.application.UserRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccessAuthenticator implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final Clock clock;

    public AccessAuthenticator(
            UserRepository users, RefreshSessionRepository sessions, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var user = users.find(jwt.getSubject()).orElseThrow(this::invalid);
        if (!user.isActive()
                || user.securityVersion() != ((Number) jwt.getClaim("version")).longValue()
                || !sessions.familyActive(
                        jwt.getClaimAsString("family"), user.id(), clock.instant()))
            throw invalid();
        return new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + user.type())), user.id());
    }

    private OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
    }
}
