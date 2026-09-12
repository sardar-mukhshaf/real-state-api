package com.realestate.shared.domain;

public class BusinessException extends RuntimeException {
    public enum Kind {
        VALIDATION,
        NOT_FOUND,
        CONFLICT,
        UNAUTHORIZED,
        FORBIDDEN,
        RATE_LIMIT,
        UNAVAILABLE
    }

    private final Kind kind;

    public BusinessException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(Kind.VALIDATION, message);
    }

    public static BusinessException missing(String resource) {
        return new BusinessException(Kind.NOT_FOUND, resource + " not found");
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(Kind.CONFLICT, message);
    }

    public static BusinessException unauthorized() {
        return new BusinessException(Kind.UNAUTHORIZED, "Invalid credentials or session");
    }

    public static BusinessException forbidden() {
        return new BusinessException(Kind.FORBIDDEN, "Access denied");
    }
}
