package com.realestate.auth.domain;

import com.realestate.shared.domain.BusinessException;

/** Replay revocation must commit even though the HTTP request fails. */
public final class SessionReplayException extends BusinessException {
    public SessionReplayException() {
        super(Kind.UNAUTHORIZED, "Invalid credentials or session");
    }
}
