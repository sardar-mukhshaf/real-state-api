package com.realestate.transactions.application;

import java.util.Optional;

public interface IdempotencyStore {
    record Entry(String fingerprint, String resultId) {}

    void lock(String key);

    Optional<Entry> find(String key);

    void save(String key, String fingerprint, String resultId);
}
