package com.realestate.auth.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SessionCleanup {
    private final JdbcTemplate jdbc;

    public SessionCleanup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${app.sessions.cleanup-interval-ms:3600000}")
    @Transactional
    public void removeExpiredSessions() {
        jdbc.update(
                """
            DELETE FROM "RefreshSession" WHERE id IN (
              SELECT id FROM "RefreshSession" WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days'
              ORDER BY expires_at LIMIT 1000 FOR UPDATE SKIP LOCKED)
            """);
    }
}
