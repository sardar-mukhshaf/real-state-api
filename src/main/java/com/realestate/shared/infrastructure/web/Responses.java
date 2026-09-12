package com.realestate.shared.infrastructure.web;

import java.util.*;
import org.springframework.http.ResponseEntity;

public final class Responses {
    private Responses() {}

    public static Map<String, Object> map(Object... pairs) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2)
            if (pairs[i + 1] != null) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    public static ResponseEntity<Map<String, Object>> ok(Object data, String message) {
        return response(data, message, 200);
    }

    public static ResponseEntity<Map<String, Object>> created(Object data, String message) {
        return response(data, message, 201);
    }

    public static ResponseEntity<Map<String, Object>> response(
            Object data, String message, int status) {
        return ResponseEntity.status(status)
                .body(
                        map(
                                "title", "Success", "status", status, "type", "success", "success",
                                true, "message", message, "data", data));
    }
}
