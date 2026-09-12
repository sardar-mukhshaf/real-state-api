package com.realestate.shared.application;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class Hashes {
    private Hashes() {}

    public static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
