package com.realestate.auth.application;

public interface PasswordHasher {
    String hash(String password);

    boolean matches(String password, String hash);

    boolean needsUpgrade(String hash);
}
