package com.realestate.auth.domain;

import com.realestate.shared.domain.BusinessException;

public record Device(String id) {
    public Device {
        if (id == null || !id.matches("[A-Za-z0-9_-]{8,128}"))
            throw BusinessException.invalid(
                    "x-device-id must contain 8 to 128 letters, digits, underscores or hyphens");
    }
}
