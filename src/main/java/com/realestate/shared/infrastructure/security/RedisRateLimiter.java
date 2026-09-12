package com.realestate.shared.infrastructure.security;

import com.realestate.shared.application.Hashes;
import com.realestate.shared.application.RateLimiter;
import com.realestate.shared.domain.*;
import com.realestate.shared.infrastructure.configuration.AppProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter implements RateLimiter {
    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(
                    """
        local count=redis.call('INCR',KEYS[1])
        if count==1 then redis.call('EXPIRE',KEYS[1],ARGV[2]) end
        if count>tonumber(ARGV[1]) then return math.max(1,redis.call('TTL',KEYS[1])) end
        return 0
        """,
                    Long.class);
    private static final Map<String, Integer> DEFAULTS =
            Map.of(
                    "GLOBAL",
                    300,
                    "LOGIN",
                    10,
                    "REGISTER",
                    5,
                    "REFRESH",
                    30,
                    "PASSWORD_RESET",
                    5,
                    "API",
                    120,
                    "UPLOAD",
                    10,
                    "STRICT",
                    10);
    private final StringRedisTemplate redis;
    private final AppProperties properties;
    private final MeterRegistry metrics;

    public RedisRateLimiter(
            StringRedisTemplate redis, AppProperties properties, MeterRegistry metrics) {
        this.redis = redis;
        this.properties = properties;
        this.metrics = metrics;
    }

    public void check(String policy, String identifier) {
        if (!DEFAULTS.containsKey(policy))
            throw new IllegalArgumentException("Unknown rate-limit policy");
        var configured =
                properties
                        .rateLimit()
                        .getOrDefault(
                                policy.toLowerCase(Locale.ROOT),
                                new AppProperties.Limit(DEFAULTS.get(policy), 60));
        int limit = configured.limit();
        int seconds = configured.windowSeconds();
        Long retry;
        try {
            retry =
                    redis.execute(
                            SCRIPT,
                            List.of(
                                    "real-estate:"
                                            + properties.environment()
                                            + ":rate:"
                                            + policy
                                            + ":"
                                            + Hashes.sha256(identifier)),
                            Integer.toString(limit),
                            Integer.toString(seconds));
        } catch (RuntimeException ex) {
            metrics.counter("security.rate_limit.unavailable").increment();
            throw new BusinessException(
                    BusinessException.Kind.UNAVAILABLE,
                    "Request protection temporarily unavailable");
        }
        if (retry == null)
            throw new BusinessException(
                    BusinessException.Kind.UNAVAILABLE,
                    "Request protection temporarily unavailable");
        if (retry > 0) {
            metrics.counter("security.rate_limit.rejected", "policy", policy).increment();
            throw new RateLimitException(retry);
        }
    }
}
