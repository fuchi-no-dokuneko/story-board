package dev.storyblock.monitor;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.validator.EvidenceSpans;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public record MonitorEvidence(
        Ids.BlockId blockId,
        int startGrapheme,
        int endGrapheme,
        String quote,
        String quoteHash
) {
    private static final Set<String> FIELDS = Set.of(
            "block_id", "start_grapheme", "end_grapheme", "quote", "quote_hash"
    );

    public MonitorEvidence {
        java.util.Objects.requireNonNull(blockId, "blockId");
        if (startGrapheme < 0 || endGrapheme <= startGrapheme) {
            throw new IllegalArgumentException("Monitor evidence range is invalid");
        }
        if (quote == null || quote.isEmpty()) {
            throw new IllegalArgumentException("Monitor evidence quote cannot be empty");
        }
        if (!EvidenceSpans.quoteHash(quote).equals(quoteHash)) {
            throw new IllegalArgumentException("Monitor evidence quote hash does not match");
        }
    }

    public static MonitorEvidence fromCanonical(Map<String, Object> value) {
        if (!value.keySet().equals(FIELDS)) {
            throw new IllegalArgumentException("Monitor evidence fields are invalid");
        }
        return new MonitorEvidence(
                new Ids.BlockId(string(value, "block_id")),
                exactInt(value.get("start_grapheme"), "start_grapheme"),
                exactInt(value.get("end_grapheme"), "end_grapheme"),
                string(value, "quote"),
                string(value, "quote_hash")
        );
    }

    public boolean matches(String text) {
        return EvidenceSpans.matches(text, canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "block_id", blockId.value(),
                "end_grapheme", endGrapheme,
                "quote", quote,
                "quote_hash", quoteHash,
                "start_grapheme", startGrapheme
        ), "monitor_evidence");
    }

    private static String string(Map<String, Object> value, String field) {
        Object raw = value.get(field);
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException("Monitor evidence " + field + " must be a string");
        }
        return text;
    }

    private static int exactInt(Object value, String field) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Monitor evidence " + field + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "Monitor evidence " + field + " must be an exact integer", failure
            );
        }
    }
}
