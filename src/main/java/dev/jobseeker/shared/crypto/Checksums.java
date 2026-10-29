package dev.jobseeker.shared.crypto;

import java.security.MessageDigest;
import java.util.HexFormat;

public final class Checksums {
    private Checksums() {}

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
