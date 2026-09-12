package com.realestate.transactions.infrastructure.persistence;

import com.realestate.transactions.application.IdempotencyStore;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresIdempotencyStore implements IdempotencyStore {
    private final JdbcTemplate jdbc;

    public PostgresIdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lock(String key) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> {}, key);
    }

    public Optional<Entry> find(String key) {
        return jdbc
                .query(
                        "SELECT fingerprint,result_id FROM \"IdempotencyRecord\" WHERE key=?",
                        (rs, row) -> new Entry(rs.getString(1), rs.getString(2)),
                        key)
                .stream()
                .findFirst();
    }

    public void save(String key, String fingerprint, String resultId) {
        jdbc.update(
                "INSERT INTO \"IdempotencyRecord\" (key,fingerprint,result_id) VALUES (?,?,?)",
                key,
                fingerprint,
                resultId);
    }
}
