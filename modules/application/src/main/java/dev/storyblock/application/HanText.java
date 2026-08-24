package dev.storyblock.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Objects;

public final class HanText {
    private HanText() {
    }

    public static String characters(String text) {
        Objects.requireNonNull(text, "text");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder result = new StringBuilder(normalized.length());
        normalized.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint)
                        == Character.UnicodeScript.HAN)
                .forEach(result::appendCodePoint);
        return result.toString();
    }

    public static int count(String text) {
        String characters = characters(text);
        return characters.codePointCount(0, characters.length());
    }

    public static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    characters(text).getBytes(StandardCharsets.UTF_8)
            );
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }
}
