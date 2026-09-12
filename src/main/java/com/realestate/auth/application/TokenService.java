package com.realestate.auth.application;

import java.time.Instant;

public interface TokenService {
    record Claims(
            String userId,
            String tokenId,
            String familyId,
            String deviceId,
            long version,
            Instant expiresAt) {}

    String issue(
            String userId,
            String tokenId,
            String familyId,
            String deviceId,
            long version,
            boolean refresh);

    Claims verifyRefresh(String raw);

    long accessSeconds();

    long refreshSeconds();
}
