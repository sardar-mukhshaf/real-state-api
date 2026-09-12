package com.realestate.users.application;

import com.realestate.auth.application.PasswordHasher;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.*;
import com.realestate.users.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final TenantRepository tenants;
    private final LandlordRepository landlords;
    private final PasswordHasher passwords;
    private final Clock clock;
    private final UserSecurity security;

    public UserService(
            UserRepository users,
            TenantRepository tenants,
            LandlordRepository landlords,
            PasswordHasher passwords,
            Clock clock,
            UserSecurity security) {
        this.users = users;
        this.tenants = tenants;
        this.landlords = landlords;
        this.passwords = passwords;
        this.clock = clock;
        this.security = security;
    }

    public static String email(String value) {
        Rules.text(value, "Email", 3, 254);
        if (!value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw BusinessException.invalid("Invalid email");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public User create(
            String firstName, String lastName, String email, String password, UserRole role) {
        Rules.text(firstName, "First name", 1, 100);
        Rules.text(lastName, "Last name", 1, 100);
        Rules.password(password);
        String normalized = email(email);
        if (users.first("email", normalized).isPresent())
            throw BusinessException.conflict("Email already exists");
        Instant now = clock.instant();
        var user =
                users.save(
                        new User(
                                UUID.randomUUID().toString(),
                                firstName,
                                lastName,
                                normalized,
                                passwords.hash(password),
                                false,
                                false,
                                true,
                                role.name(),
                                0,
                                now,
                                now));
        if (role == UserRole.TENANT) tenants.save(new Tenant(user.id()));
        if (role == UserRole.LANDLORD) landlords.save(new Landlord(user.id()));
        return user;
    }

    public User get(String id) {
        return users.require(Rules.id(id));
    }

    public List<User> list(UserRole role, int page, int size) {
        return users.query(Query.where("type", role.name()).page(page, size));
    }

    @Transactional
    public User update(String id, Changes patch, UserRole expected) {
        patch.only("first_name", "last_name", "email", "password", "type");
        var old = users.requireLocked(Rules.id(id));
        if (!old.type().equals(expected.name())) throw BusinessException.missing(expected.name());
        if (patch.has("type") && !patch.string("type", old.type()).equals(old.type()))
            throw BusinessException.invalid("Role cannot be changed here");
        String first =
                Rules.text(patch.string("first_name", old.firstName()), "First name", 1, 100);
        String last = Rules.text(patch.string("last_name", old.lastName()), "Last name", 1, 100);
        String email = email(patch.string("email", old.email()));
        String hash = old.passwordHash();
        long version = old.securityVersion();
        if (patch.has("password")) {
            String password = patch.string("password", null);
            Rules.password(password);
            hash = passwords.hash(password);
            version++;
        }
        var result =
                users.save(
                        new User(
                                old.id(),
                                first,
                                last,
                                email,
                                hash,
                                old.emailVerified(),
                                old.rememberMe(),
                                old.isActive(),
                                old.type(),
                                version,
                                old.createdAt(),
                                clock.instant()));
        if (patch.has("password")) security.revokeSessions(id, clock.instant());
        return result;
    }

    @Transactional
    public boolean delete(String id, UserRole expected) {
        var user = users.requireLocked(Rules.id(id));
        if (!user.type().equals(expected.name())) throw BusinessException.missing(expected.name());
        users.delete(id);
        return true;
    }

    public static Map<String, Object> view(User u) {
        return Data.map(
                "id",
                u.id(),
                "first_name",
                u.firstName(),
                "last_name",
                u.lastName(),
                "email",
                u.email(),
                "type",
                u.type(),
                "email_verified",
                u.emailVerified(),
                "remember_me",
                u.rememberMe(),
                "is_active",
                u.isActive(),
                "created_at",
                u.createdAt(),
                "updated_at",
                u.updatedAt());
    }

    public static Map<String, Object> contact(User u) {
        return Data.map(
                "id",
                u.id(),
                "first_name",
                u.firstName(),
                "last_name",
                u.lastName(),
                "email",
                u.email());
    }
}
