package dev.storyblock.domain;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** A portable image artifact referenced by a narrative block. */
public record BlockImage(
        Ids.ArtifactId artifactId,
        String contentHash,
        String mediaType,
        int widthPixels,
        int heightPixels,
        String altText
) {
    public static final String EXTENSION_KEY = "storyblock.image";
    public static final int MAX_DIMENSION_PIXELS = 8_192;
    public static final int MAX_ALT_TEXT_GRAPHEMES = 500;

    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> MEDIA_TYPES = Set.of("image/jpeg", "image/png");
    private static final Set<String> FIELDS = Set.of(
            "artifact_id", "content_hash", "media_type", "width_px", "height_px",
            "alt_text"
    );

    public BlockImage {
        Objects.requireNonNull(artifactId, "artifactId");
        if (contentHash == null || !SHA_256.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Image content hash must be lowercase SHA-256");
        }
        if (!MEDIA_TYPES.contains(mediaType)) {
            throw new IllegalArgumentException("Image media type must be image/jpeg or image/png");
        }
        if (widthPixels < 1 || widthPixels > MAX_DIMENSION_PIXELS
                || heightPixels < 1 || heightPixels > MAX_DIMENSION_PIXELS) {
            throw new IllegalArgumentException("Image dimensions must be between 1 and 8192 pixels");
        }
        if ((long) widthPixels * heightPixels > 40_000_000L) {
            throw new IllegalArgumentException("Image pixel count exceeds the safety limit");
        }
        Objects.requireNonNull(altText, "altText");
        altText = Normalizer.normalize(altText, Normalizer.Form.NFC).strip();
        if (altText.isEmpty()
                || UnicodeText.graphemeCount(altText) > MAX_ALT_TEXT_GRAPHEMES) {
            throw new IllegalArgumentException(
                    "Image alt text must contain 1 to 500 graphemes"
            );
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("artifact_id", artifactId.value());
        value.put("content_hash", contentHash);
        value.put("media_type", mediaType);
        value.put("width_px", widthPixels);
        value.put("height_px", heightPixels);
        value.put("alt_text", altText);
        return Map.copyOf(value);
    }

    public Map<String, Object> attachTo(Map<String, Object> extensions) {
        Objects.requireNonNull(extensions, "extensions");
        Map<String, Object> updated = new LinkedHashMap<>(extensions);
        updated.put(EXTENSION_KEY, canonicalValue());
        return Map.copyOf(updated);
    }

    public static Optional<BlockImage> fromExtensions(Map<String, Object> extensions) {
        Objects.requireNonNull(extensions, "extensions");
        Object raw = extensions.get(EXTENSION_KEY);
        if (raw == null) {
            return Optional.empty();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(EXTENSION_KEY + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String string) || !FIELDS.contains(string)) {
                throw new IllegalArgumentException(
                        EXTENSION_KEY + " contains an unknown field " + key
                );
            }
        }
        for (String field : FIELDS) {
            if (!map.containsKey(field)) {
                throw new IllegalArgumentException(EXTENSION_KEY + " is missing " + field);
            }
        }
        return Optional.of(new BlockImage(
                new Ids.ArtifactId(string(map, "artifact_id")),
                string(map, "content_hash"),
                string(map, "media_type"),
                integer(map, "width_px"),
                integer(map, "height_px"),
                string(map, "alt_text")
        ));
    }

    private static String string(Map<?, ?> map, String field) {
        Object value = map.get(field);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(EXTENSION_KEY + "." + field + " must be a string");
        }
        return text;
    }

    private static int integer(Map<?, ?> map, String field) {
        Object value = map.get(field);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(EXTENSION_KEY + "." + field + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(
                    EXTENSION_KEY + "." + field + " must be an exact integer",
                    failure
            );
        }
    }
}
