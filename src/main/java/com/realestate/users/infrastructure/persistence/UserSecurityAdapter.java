package com.realestate.users.infrastructure.persistence;

import com.realestate.users.application.UserSecurity;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserSecurityAdapter implements UserSecurity {
    private final JdbcTemplate jdbc;

    public UserSecurityAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void revokeSessions(String userId, Instant now) {
        jdbc.update(
                "UPDATE \"RefreshSession\" SET revoked_at=?,updated_at=? WHERE user_id=? AND revoked_at IS NULL",
                Timestamp.from(now),
                Timestamp.from(now),
                userId);
    }
}
