package com.realestate.shared.domain;

public final class RateLimitException extends BusinessException {
    private final long retryAfter;

    public RateLimitException(long retryAfter) {
        super(Kind.RATE_LIMIT, "Too many requests");
        this.retryAfter = retryAfter;
    }

    public long retryAfter() {
        return retryAfter;
    }
}
