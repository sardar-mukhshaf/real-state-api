package com.realestate.shared.application;

import com.realestate.shared.domain.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/** Presence-aware update command: omitted fields survive; explicit null is rejected. */
public record Changes(Map<String, Object> values) {
    public Changes {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public void only(String... fields) {
        var allowed = Set.of(fields);
        if (values.keySet().stream().anyMatch(k -> !allowed.contains(k)))
            throw BusinessException.invalid("Unknown or read-only field");
        if (values.containsValue(null))
            throw BusinessException.invalid("Explicit null is not allowed; omit unchanged fields");
    }

    public boolean has(String field) {
        return values.containsKey(field);
    }

    public String string(String key, String fallback) {
        if (!has(key)) return fallback;
        if (!(values.get(key) instanceof String value))
            throw BusinessException.invalid(key + " must be a string");
        return value;
    }

    public BigDecimal decimal(String key, BigDecimal fallback) {
        if (!has(key)) return fallback;
        try {
            String raw = values.get(key).toString();
            if (raw.length() > 64) throw new IllegalArgumentException();
            BigDecimal value = new BigDecimal(raw);
            if (value.scale() < -19 || value.scale() > 8 || value.precision() > 24)
                throw new IllegalArgumentException();
            return value;
        } catch (RuntimeException ex) {
            throw BusinessException.invalid(key + " must be numeric");
        }
    }

    public int integer(String key, int fallback) {
        try {
            return decimal(key, BigDecimal.valueOf(fallback)).intValueExact();
        } catch (ArithmeticException ex) {
            throw BusinessException.invalid(key + " must be an integer");
        }
    }

    public boolean bool(String key, boolean fallback) {
        if (!has(key)) return fallback;
        if (values.get(key) instanceof Boolean value) return value;
        throw BusinessException.invalid(key + " must be a boolean");
    }

    public Instant instant(String key, Instant fallback) {
        if (!has(key)) return fallback;
        String value = string(key, null);
        try {
            return value.length() == 10
                    ? LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC)
                    : Instant.parse(value);
        } catch (RuntimeException ex) {
            throw BusinessException.invalid(key + " must be an ISO date or UTC timestamp");
        }
    }

    public <E extends Enum<E>> E enumeration(String key, E fallback, Class<E> type) {
        if (!has(key)) return fallback;
        try {
            return Enum.valueOf(type, string(key, null));
        } catch (RuntimeException ex) {
            throw BusinessException.invalid("Invalid " + key);
        }
    }
}
