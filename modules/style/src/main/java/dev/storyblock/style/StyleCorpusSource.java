package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleCorpusSource(
        String sourceId,
        String contentHash,
        StyleCorpusSourceKind kind,
        String provenance,
        String license,
        String ownership
) {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "source_id", "content_hash", "kind", "provenance", "license", "ownership"
    );

    public StyleCorpusSource {
        if (sourceId == null || !ID.matcher(sourceId).matches()) {
            throw new IllegalArgumentException("Style corpus source ID is invalid");
        }
        if (contentHash == null || !HASH.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Style corpus hash must be lowercase SHA-256");
        }
        java.util.Objects.requireNonNull(kind, "kind");
        provenance = requireText(provenance, "provenance", 2_000);
        license = requireText(license, "license", 500);
        ownership = requireText(ownership, "ownership", 500);
    }

    public static StyleCorpusSource fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_corpus_source");
        return new StyleCorpusSource(
                StyleCanonical.string(value, "source_id", "style_corpus_source"),
                StyleCanonical.string(value, "content_hash", "style_corpus_source"),
                StyleCorpusSourceKind.fromCanonicalName(StyleCanonical.string(
                        value, "kind", "style_corpus_source"
                )),
                StyleCanonical.string(value, "provenance", "style_corpus_source"),
                StyleCanonical.string(value, "license", "style_corpus_source"),
                StyleCanonical.string(value, "ownership", "style_corpus_source")
        );
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "content_hash", contentHash,
                "kind", kind.canonicalName(),
                "license", license,
                "ownership", ownership,
                "provenance", provenance,
                "source_id", sourceId
        ), "style_corpus_source");
    }

    private static String requireText(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException("Style corpus " + field + " is invalid");
        }
        return value;
    }
}
