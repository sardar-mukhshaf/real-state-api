package com.realestate;

import static org.assertj.core.api.Assertions.*;

import com.realestate.shared.domain.RateLimitException;
import com.realestate.shared.infrastructure.configuration.AppProperties;
import com.realestate.shared.infrastructure.security.RedisRateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

class RedisRateLimiterIT {
    @Test
    void sharedRedisCounterIsAtomicAcrossTwoInstances() throws Exception {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the Redis integration test");
        try (var container = new GenericContainer<>("redis:7.4.8-alpine").withExposedPorts(6379)) {
            container.start();
            var connection =
                    new LettuceConnectionFactory(
                            container.getHost(), container.getMappedPort(6379));
            connection.afterPropertiesSet();
            connection.start();
            try {
                var template = new StringRedisTemplate(connection);
                template.afterPropertiesSet();
                var env =
                        new AppProperties(
                                null,
                                null,
                                null,
                                List.of(),
                                "integration",
                                Map.of("login", new AppProperties.Limit(3, 60)));
                var a = new RedisRateLimiter(template, env, new SimpleMeterRegistry());
                var b = new RedisRateLimiter(template, env, new SimpleMeterRegistry());
                var outcomes = new ArrayList<Future<Boolean>>();
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    for (int i = 0; i < 12; i++) {
                        var limiter = i % 2 == 0 ? a : b;
                        outcomes.add(
                                executor.submit(
                                        () -> {
                                            try {
                                                limiter.check("LOGIN", "account:test");
                                                return true;
                                            } catch (RateLimitException ex) {
                                                assertThat(ex.retryAfter()).isBetween(1L, 60L);
                                                return false;
                                            }
                                        }));
                    }
                    int accepted = 0;
                    for (var outcome : outcomes) if (outcome.get()) accepted++;
                    assertThat(accepted).isEqualTo(3);
                }
            } finally {
                connection.destroy();
            }
        }
    }
}
