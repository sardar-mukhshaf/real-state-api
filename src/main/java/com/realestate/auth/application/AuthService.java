package com.realestate.auth.application;

import com.realestate.auth.domain.*;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.*;
import com.realestate.users.application.*;
import com.realestate.users.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserService userService;
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordHasher passwords;
    private final TokenService tokens;
    private final RateLimiter limits;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(
            UserService userService,
            UserRepository users,
            RefreshSessionRepository sessions,
            PasswordHasher passwords,
            TokenService tokens,
            RateLimiter limits,
            Clock clock) {
        this.userService = userService;
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tokens = tokens;
        this.limits = limits;
        this.clock = clock;
        dummyHash = passwords.hash(UUID.randomUUID().toString());
    }

    @Transactional
    public Map<String, Object> register(
            String first,
            String last,
            String email,
            String password,
            String requestedRole,
            Device device) {
        if (requestedRole != null && !requestedRole.equals("USER"))
            throw BusinessException.forbidden();
        limits.check("REGISTER", UserService.email(email));
        return issue(
                userService.create(first, last, email, password, UserRole.USER),
                device,
                UUID.randomUUID().toString());
    }

    @Transactional
    public Map<String, Object> login(String email, String password, Device device) {
        String normalized = UserService.email(email);
        limits.check("LOGIN", normalized);
        var found = users.first("email", normalized);
        if (found.isEmpty()) {
            passwords.matches(password, dummyHash);
            throw BusinessException.unauthorized();
        }
        var user = users.requireLocked(found.get().id());
        if (!passwords.matches(password, user.passwordHash()) || !user.isActive())
            throw BusinessException.unauthorized();
        if (passwords.needsUpgrade(user.passwordHash()))
            user = saveSecurity(user, passwords.hash(password), user.securityVersion());
        return issue(user, device, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = SessionReplayException.class)
    public Map<String, Object> refresh(String raw, Device device) {
        var claims = tokens.verifyRefresh(raw);
        if (!device.id().equals(claims.deviceId())) throw BusinessException.unauthorized();
        // Lock the user before the token in every session mutation to serialize concurrent
        // rotation.
        var user = users.lock(claims.userId()).orElseThrow(BusinessException::unauthorized);
        var old = sessions.lock(claims.tokenId()).orElseThrow(BusinessException::unauthorized);
        if (!old.userId().equals(user.id())
                || !old.deviceId().equals(device.id())
                || !old.familyId().equals(claims.familyId())
                || !MessageDigest.isEqual(
                        old.tokenHash().getBytes(StandardCharsets.US_ASCII),
                        fingerprint(raw).getBytes(StandardCharsets.US_ASCII)))
            throw BusinessException.unauthorized();
        if (old.revokedAt() != null) {
            revokeFamily(old.familyId());
            throw new SessionReplayException();
        }
        if (!user.isActive()
                || user.securityVersion() != claims.version()
                || !old.expiresAt().isAfter(clock.instant()))
            throw BusinessException.unauthorized();
        var issued = issue(user, device, old.familyId());
        sessions.save(revoked(old, (String) issued.get("_refresh_id")));
        return issued;
    }

    @Transactional
    public boolean logout(String userId, String familyId, boolean all) {
        var user = users.requireLocked(userId);
        if (all) {
            saveSecurity(user, user.passwordHash(), user.securityVersion() + 1);
            revokeUser(userId);
        } else revokeFamily(familyId);
        return true;
    }

    @Transactional
    public boolean changePassword(String userId, String current, String next) {
        var user = users.requireLocked(userId);
        if (!passwords.matches(current, user.passwordHash()))
            throw BusinessException.unauthorized();
        Rules.password(next);
        if (passwords.matches(next, user.passwordHash()))
            throw BusinessException.invalid("Choose a different password");
        saveSecurity(user, passwords.hash(next), user.securityVersion() + 1);
        revokeUser(userId);
        return true;
    }

    private User saveSecurity(User u, String hash, long version) {
        return users.save(
                new User(
                        u.id(),
                        u.firstName(),
                        u.lastName(),
                        u.email(),
                        hash,
                        u.emailVerified(),
                        u.rememberMe(),
                        u.isActive(),
                        u.type(),
                        version,
                        u.createdAt(),
                        clock.instant()));
    }

    private Map<String, Object> issue(User user, Device device, String family) {
        String id = UUID.randomUUID().toString();
        String refresh =
                tokens.issue(user.id(), id, family, device.id(), user.securityVersion(), true);
        var now = clock.instant();
        sessions.save(
                new RefreshSession(
                        id,
                        user.id(),
                        device.id(),
                        family,
                        fingerprint(refresh),
                        now.plusSeconds(tokens.refreshSeconds()),
                        null,
                        null,
                        now,
                        now));
        return Data.map(
                "user",
                UserService.view(user),
                "token",
                tokens.issue(
                        user.id(),
                        UUID.randomUUID().toString(),
                        family,
                        device.id(),
                        user.securityVersion(),
                        false),
                "refresh_token",
                refresh,
                "expires_in",
                tokens.accessSeconds(),
                "token_type",
                "Bearer",
                "_refresh_id",
                id);
    }

    private RefreshSession revoked(RefreshSession s, String replacement) {
        return new RefreshSession(
                s.id(),
                s.userId(),
                s.deviceId(),
                s.familyId(),
                s.tokenHash(),
                s.expiresAt(),
                clock.instant(),
                replacement,
                s.createdAt(),
                clock.instant());
    }

    private void revokeFamily(String family) {
        sessions.revokeFamily(family, clock.instant());
    }

    private void revokeUser(String userId) {
        sessions.revokeUser(userId, clock.instant());
    }

    public static String fingerprint(String raw) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
