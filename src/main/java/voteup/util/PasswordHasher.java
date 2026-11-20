package voteup.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Simple password hashing for the local VoteUp demo (SHA-256).
 * Not intended as production-grade password storage.
 */
public final class PasswordHasher {

    private PasswordHasher() {
        // utility
    }

    /**
     * Hashes a plaintext password with SHA-256.
     *
     * @param plaintext password text
     * @return lowercase hex digest
     */
    public static String hash(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * @param plaintext password text
     * @param expectedHash stored hash
     * @return {@code true} if the hash of {@code plaintext} matches
     */
    public static boolean matches(String plaintext, String expectedHash) {
        if (plaintext == null || expectedHash == null) {
            return false;
        }
        return hash(plaintext).equals(expectedHash);
    }
}
