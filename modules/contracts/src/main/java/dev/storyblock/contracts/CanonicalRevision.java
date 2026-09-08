package dev.storyblock.contracts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class CanonicalRevision {
    public static final String SCHEMA_VERSION = "1.0.0";

    static final Set<String> ROOT_REQUIRED = Set.of(
            "schema_version",
            "novel_id",
            "revision_id",
            "parent_revision_id",
            "chapters",
            "created_at"
    );
    static final Set<String> ROOT_OPTIONAL = Set.of("extensions");
    static final Set<String> CHAPTER_REQUIRED = Set.of("id", "order_key", "scenes");
    static final Set<String> CHAPTER_OPTIONAL = Set.of("title", "extensions");
    static final Set<String> SCENE_REQUIRED = Set.of(
            "id",
            "chapter_id",
            "order_key",
            "transition_mode",
            "blocks"
    );
    static final Set<String> SCENE_OPTIONAL = Set.of(
            "title",
            "initial_meta",
            "extensions"
    );
    static final Set<String> BLOCK_REQUIRED = Set.of(
            "id",
            "block_version_id",
            "order_key",
            "text",
            "meta"
    );
    static final Set<String> BLOCK_OPTIONAL = Set.of("extensions");
    static final Set<String> META_FIELDS = Set.of(
            "time",
            "location",
            "weather",
            "speech",
            "actions",
            "presence_events",
            "pov",
            "narrative_mode",
            "provenance"
    );
    static final Set<String> INITIAL_META_FIELDS = Set.of(
            "time",
            "location",
            "weather",
            "present_character_ids"
    );
    static final Set<String> TRANSITION_MODES = Set.of(
            "opening",
            "continuous",
            "cut",
            "time_skip",
            "flashback",
            "parallel"
    );
    static final Pattern EXTENSION_NAME = Pattern.compile("[a-z][a-z0-9.-]{1,63}");
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    private final Map<String, Object> canonicalContent;
    private final Map<String, Object> derivedData;
    private final byte[] canonicalBytes;
    private final String contentHash;

    private CanonicalRevision(
            Map<String, ?> canonicalContent,
            Map<String, ?> derivedData
    ) {
        this.canonicalContent = CanonicalRevisionFreezeMap.freezeMap(canonicalContent, "document");
        this.derivedData = CanonicalRevisionFreezeMap.freezeMap(derivedData, "derived");
        CanonicalRevisionValidateDocument.validateDocument(this.canonicalContent);
        this.canonicalBytes = CanonicalJson.bytes(this.canonicalContent);
        this.contentHash = CanonicalJson.hashBytes(this.canonicalBytes);
    }

    public static CanonicalRevision of(Map<String, ?> canonicalContent) {
        return new CanonicalRevision(canonicalContent, Map.of());
    }

    public static CanonicalRevision of(
            Map<String, ?> canonicalContent,
            Map<String, ?> derivedData
    ) {
        return new CanonicalRevision(canonicalContent, derivedData);
    }

    public static CanonicalRevision parseEnvelope(byte[] json) {
        Objects.requireNonNull(json, "json");
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = CanonicalJson.mapper().readValue(json, Map.class);
        envelope = new TreeMap<>(envelope);
        Object declaredHash = envelope.remove("content_hash");
        if (!(declaredHash instanceof String hash) || !SHA_256.matcher(hash).matches()) {
            throw new IllegalArgumentException("Canonical envelope has no valid content_hash");
        }

        CanonicalRevision revision = CanonicalRevision.of(envelope);
        if (!MessageDigest.isEqual(
                hash.getBytes(StandardCharsets.US_ASCII),
                revision.contentHash.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new IllegalArgumentException("Canonical envelope content_hash does not match content");
        }
        return revision;
    }

    public Map<String, Object> canonicalContent() {
        return canonicalContent;
    }

    public Map<String, Object> derivedData() {
        return derivedData;
    }

    public byte[] canonicalBytes() {
        return canonicalBytes.clone();
    }

    public String contentHash() {
        return contentHash;
    }

    public byte[] envelopeBytes() {
        return CanonicalJson.bytes(envelope());
    }

    public Map<String, Object> envelope() {
        Map<String, Object> envelope = new TreeMap<>(canonicalContent);
        envelope.put("content_hash", contentHash);
        return Collections.unmodifiableMap(envelope);
    }

    public byte[] diagnosticExportBytes() {
        Map<String, Object> export = new TreeMap<>(canonicalContent);
        export.put("content_hash", contentHash);
        if (!derivedData.isEmpty()) {
            export.put("derived", derivedData);
        }
        return CanonicalJson.bytes(export);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> requireMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        return (Map<String, Object>) map;
    }

}
