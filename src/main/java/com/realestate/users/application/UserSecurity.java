package com.realestate.users.application;

import java.time.Instant;

public interface UserSecurity {
    void revokeSessions(String userId, Instant now);
}
