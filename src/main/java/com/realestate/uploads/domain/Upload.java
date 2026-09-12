package com.realestate.uploads.domain;

import com.realestate.shared.domain.BusinessException;
import java.util.*;

public record Upload(String name, String contentType, byte[] bytes) {
    public Upload {
        if (name == null
                || name.length() > 255
                || name.contains("/")
                || name.contains("\\")
                || name.chars().anyMatch(c -> c < 32))
            throw BusinessException.invalid("Invalid filename");
        if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
            throw BusinessException.invalid("File must contain 1 byte to 5 MB");
        if (contentType == null) throw BusinessException.invalid("File MIME type is required");
        bytes = bytes.clone();
        String lower = name.toLowerCase(Locale.ROOT);
        boolean jpeg =
                bytes.length >= 3
                        && (bytes[0] & 255) == 255
                        && (bytes[1] & 255) == 216
                        && (bytes[2] & 255) == 255;
        boolean png =
                bytes.length >= 8
                        && Arrays.equals(
                                Arrays.copyOf(bytes, 8),
                                new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        boolean webp =
                bytes.length >= 12
                        && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)
                                .equals("RIFF")
                        && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII)
                                .equals("WEBP");
        boolean pdf =
                bytes.length >= 5
                        && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)
                                .equals("%PDF-");
        boolean accepted =
                (jpeg
                                && Set.of("image/jpeg", "image/jpg").contains(contentType)
                                && (lower.endsWith(".jpg") || lower.endsWith(".jpeg")))
                        || (png && "image/png".equals(contentType) && lower.endsWith(".png"))
                        || (webp && "image/webp".equals(contentType) && lower.endsWith(".webp"))
                        || (pdf && "application/pdf".equals(contentType) && lower.endsWith(".pdf"));
        if (!accepted)
            throw BusinessException.invalid(
                    "File content, MIME type and extension must match JPEG, PNG, WebP or PDF");
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    public boolean image() {
        return contentType.startsWith("image/");
    }

    @Override
    public String toString() {
        return "Upload[bytes=" + bytes.length + "]";
    }
}
