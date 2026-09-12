package com.realestate.auth.infrastructure.security;

import com.realestate.shared.infrastructure.configuration.AppProperties;
import com.realestate.users.application.*;
import com.realestate.users.domain.UserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserService users;
    private final UserRepository repository;
    private final String email;
    private final String password;

    public AdminBootstrap(UserService users, UserRepository repository, AppProperties properties) {
        this.users = users;
        this.repository = repository;
        this.email = properties.bootstrap().email();
        this.password = properties.bootstrap().password();
    }

    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.isBlank())
            throw new IllegalStateException("Both bootstrap admin settings are required");
        var existing = repository.first("email", UserService.email(email));
        if (existing.isPresent()) {
            if (!existing.get().type().equals("ADMIN"))
                throw new IllegalStateException("Bootstrap email already belongs to a non-admin");
            return;
        }
        users.create("System", "Administrator", email, password, UserRole.ADMIN);
    }
}
