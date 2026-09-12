package com.realestate.auth.application;

import com.realestate.auth.domain.RefreshSession;
import com.realestate.shared.application.Store;
import java.time.Instant;

public interface RefreshSessionRepository extends Store<RefreshSession> {
    void revokeFamily(String family, Instant now);

    void revokeUser(String userId, Instant now);

    boolean familyActive(String family, String userId, Instant now);
}
