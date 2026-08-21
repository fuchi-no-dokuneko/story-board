package dev.storyblock.validator;

import dev.storyblock.domain.UnicodeText;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class EvidenceSpans {
    private EvidenceSpans() {
    }

    public static String quoteHash(String quote) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(quote.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM does not provide SHA-256", exception);
        }
    }

    public static boolean matches(String text, Map<?, ?> evidence) {
        Object startValue = evidence.get("start_grapheme");
        Object endValue = evidence.get("end_grapheme");
        Object quoteValue = evidence.get("quote");
        Object hashValue = evidence.get("quote_hash");
        if (!(startValue instanceof Number startNumber)
                || !(endValue instanceof Number endNumber)
                || !(quoteValue instanceof String quote)
                || !(hashValue instanceof String hash)) {
            return false;
        }
        int start;
        int end;
        try {
            start = new java.math.BigDecimal(startNumber.toString()).intValueExact();
            end = new java.math.BigDecimal(endNumber.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            return false;
        }
        List<String> graphemes = UnicodeText.graphemes(text);
        if (start < 0 || end <= start || end > graphemes.size()) {
            return false;
        }
        String selected = String.join("", graphemes.subList(start, end));
        return selected.equals(quote) && quoteHash(quote).equals(hash);
    }
}
