package dev.storyblock.renderer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class DeterministicPdfRendererHash {
    static String hash(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("JVM does not provide SHA-256", failure);
        }
    }
}
