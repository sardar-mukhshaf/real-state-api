package com.realestate.shared.application;

public interface RateLimiter {
    void check(String policy, String identifier);
}
