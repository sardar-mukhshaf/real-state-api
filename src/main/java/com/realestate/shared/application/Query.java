package com.realestate.shared.application;

import com.realestate.shared.domain.BusinessException;
import java.util.*;

/** Internal query vocabulary. Controllers cannot supply persistence field names. */
public record Query(
        Map<String, Object> equal,
        Map<String, String> contains,
        int offset,
        int size,
        String order,
        boolean descending) {
    public Query {
        equal = Map.copyOf(equal);
        contains = Map.copyOf(contains);
        if (offset < 0 || size < 1 || size > 10000)
            throw BusinessException.invalid("Invalid query bounds");
    }

    public static Query all() {
        return new Query(Map.of(), Map.of(), 0, 100, "id", false);
    }

    public static Query where(String field, Object value) {
        return all().and(field, value);
    }

    public Query and(String field, Object value) {
        var next = new HashMap<>(equal);
        next.put(field, value);
        return new Query(next, contains, offset, size, order, descending);
    }

    public Query like(String field, String value) {
        var next = new HashMap<>(contains);
        next.put(field, value);
        return new Query(equal, next, offset, size, order, descending);
    }

    public Query limit(int value) {
        return new Query(equal, contains, offset, value, order, descending);
    }

    public Query page(int page, int value) {
        if (page < 0 || page > 100000 || value < 1 || value > 100)
            throw BusinessException.invalid("page >= 0; size between 1 and 100");
        return new Query(equal, contains, page * value, value, order, descending);
    }

    public Query sorted(String field, boolean desc) {
        return new Query(equal, contains, offset, size, field, desc);
    }
}
