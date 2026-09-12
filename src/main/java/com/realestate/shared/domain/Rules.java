package com.realestate.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public final class Rules {
    private Rules() {}

    public static String text(String value, String name, int min, int max) {
        if (value == null || value.trim().length() < min || value.length() > max)
            throw BusinessException.invalid(
                    name + " must contain " + min + " to " + max + " characters");
        return value;
    }

    public static String id(String value) {
        try {
            if (!UUID.fromString(value).toString().equalsIgnoreCase(value))
                throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw BusinessException.invalid("Invalid UUID");
        }
        return value;
    }

    public static BigDecimal money(BigDecimal value, boolean zeroAllowed) {
        if (value == null
                || value.signum() < 0
                || (!zeroAllowed && value.compareTo(BigDecimal.ONE) < 0))
            throw BusinessException.invalid("Amount is outside the allowed range");
        if ((long) value.precision() - value.scale() > 17 || value.stripTrailingZeros().scale() > 2)
            throw BusinessException.invalid(
                    "Money supports 17 integer digits and 2 decimal places");
        try {
            value = value.setScale(2, RoundingMode.UNNECESSARY);
            if (value.precision() > 19) throw new ArithmeticException();
            return value;
        } catch (ArithmeticException ex) {
            throw BusinessException.invalid(
                    "Money supports 17 integer digits and 2 decimal places");
        }
    }

    public static void dates(Instant start, Instant end) {
        if (start == null || end == null || end.isBefore(start))
            throw BusinessException.invalid("End date must be on or after start date");
    }

    public static void password(String password) {
        text(password, "Password", 8, 72);
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s)(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}"))
            throw BusinessException.invalid(
                    "Password needs uppercase, lowercase and a digit; maximum 72 UTF-8 bytes");
    }
}
