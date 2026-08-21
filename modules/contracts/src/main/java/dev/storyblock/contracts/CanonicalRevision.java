package dev.storyblock.contracts;

import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.StableIds;
import dev.storyblock.domain.UnicodeText;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class CanonicalRevision {
    public static final String SCHEMA_VERSION = "1.0.0";

    private static final Set<String> ROOT_REQUIRED = Set.of(
            "schema_version",
            "novel_id",
            "revision_id",
            "parent_revision_id",
            "chapters",
            "created_at"
    );
    private static final Set<String> ROOT_OPTIONAL = Set.of("extensions");
    private static final Set<String> CHAPTER_REQUIRED = Set.of("id", "order_key", "scenes");
    private static final Set<String> CHAPTER_OPTIONAL = Set.of("title", "extensions");
    private static final Set<String> SCENE_REQUIRED = Set.of(
            "id",
            "chapter_id",
            "order_key",
            "transition_mode",
            "blocks"
    );
    private static final Set<String> SCENE_OPTIONAL = Set.of(
            "title",
            "initial_meta",
            "extensions"
    );
    private static final Set<String> BLOCK_REQUIRED = Set.of(
            "id",
            "block_version_id",
            "order_key",
            "text",
            "meta"
    );
    private static final Set<String> BLOCK_OPTIONAL = Set.of("extensions");
    private static final Set<String> META_FIELDS = Set.of(
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
    private static final Set<String> INITIAL_META_FIELDS = Set.of(
            "time",
            "location",
            "weather",
            "present_character_ids"
    );
    private static final Set<String> TRANSITION_MODES = Set.of(
            "opening",
            "continuous",
            "cut",
            "time_skip",
            "flashback",
            "parallel"
    );
    private static final Pattern EXTENSION_NAME = Pattern.compile("[a-z][a-z0-9.-]{1,63}");
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    private final Map<String, Object> canonicalContent;
    private final Map<String, Object> derivedData;
    private final byte[] canonicalBytes;
    private final String contentHash;

    private CanonicalRevision(
            Map<String, ?> canonicalContent,
            Map<String, ?> derivedData
    ) {
        this.canonicalContent = freezeMap(canonicalContent, "document");
        this.derivedData = freezeMap(derivedData, "derived");
        validateDocument(this.canonicalContent);
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

    private static void validateDocument(Map<String, Object> document) {
        validateKeys(document, ROOT_REQUIRED, ROOT_OPTIONAL, "document");
        requireExactString(document, "schema_version", SCHEMA_VERSION, "document");
        StableIds.require(requireString(document, "novel_id", "document"), "nov");
        StableIds.require(requireString(document, "revision_id", "document"), "rev");

        Object parentRevision = document.get("parent_revision_id");
        if (parentRevision != null) {
            StableIds.require(requireString(document, "parent_revision_id", "document"), "rev");
        }

        String createdAt = requireString(document, "created_at", "document");
        try {
            Instant instant = Instant.parse(createdAt);
            if (!instant.toString().equals(createdAt)) {
                throw new IllegalArgumentException("document.created_at must use canonical UTC form");
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("document.created_at must be an ISO-8601 instant", exception);
        }

        validateExtensions(document.get("extensions"), "document.extensions");
        List<Object> chapters = requireList(document, "chapters", "document");
        for (int index = 0; index < chapters.size(); index++) {
            validateChapter(requireMap(chapters.get(index), "chapter[" + index + "]"), index);
        }
    }

    private static void validateChapter(Map<String, Object> chapter, int chapterIndex) {
        String path = "chapter[" + chapterIndex + "]";
        validateKeys(chapter, CHAPTER_REQUIRED, CHAPTER_OPTIONAL, path);
        StableIds.require(requireString(chapter, "id", path), "ch");
        new OrderKey(requireString(chapter, "order_key", path));
        optionalString(chapter, "title", path);
        validateExtensions(chapter.get("extensions"), path + ".extensions");

        List<Object> scenes = requireList(chapter, "scenes", path);
        for (int index = 0; index < scenes.size(); index++) {
            validateScene(requireMap(scenes.get(index), path + ".scene[" + index + "]"), path, index);
        }
    }

    private static void validateScene(
            Map<String, Object> scene,
            String chapterPath,
            int sceneIndex
    ) {
        String path = chapterPath + ".scene[" + sceneIndex + "]";
        validateKeys(scene, SCENE_REQUIRED, SCENE_OPTIONAL, path);
        StableIds.require(requireString(scene, "id", path), "scn");
        StableIds.require(requireString(scene, "chapter_id", path), "ch");
        new OrderKey(requireString(scene, "order_key", path));
        requireExactStringInSet(scene, "transition_mode", TRANSITION_MODES, path);
        optionalString(scene, "title", path);
        validateExtensions(scene.get("extensions"), path + ".extensions");
        if (scene.containsKey("initial_meta")) {
            Map<String, Object> initialMeta = requireMap(scene.get("initial_meta"), path + ".initial_meta");
            validateKeys(initialMeta, Set.of(), INITIAL_META_FIELDS, path + ".initial_meta");
        }

        List<Object> blocks = requireList(scene, "blocks", path);
        for (int index = 0; index < blocks.size(); index++) {
            validateBlock(requireMap(blocks.get(index), path + ".block[" + index + "]"), path, index);
        }
    }

    private static void validateBlock(
            Map<String, Object> block,
            String scenePath,
            int blockIndex
    ) {
        String path = scenePath + ".block[" + blockIndex + "]";
        validateKeys(block, BLOCK_REQUIRED, BLOCK_OPTIONAL, path);
        StableIds.require(requireString(block, "id", path), "blk");
        StableIds.require(requireString(block, "block_version_id", path), "blv");
        new OrderKey(requireString(block, "order_key", path));
        UnicodeText.validateBlock(requireString(block, "text", path));
        validateExtensions(block.get("extensions"), path + ".extensions");

        Map<String, Object> meta = requireMap(block.get("meta"), path + ".meta");
        validateKeys(meta, Set.of(), META_FIELDS, path + ".meta");
    }

    private static void validateKeys(
            Map<String, Object> object,
            Set<String> required,
            Set<String> optional,
            String path
    ) {
        for (String key : required) {
            if (!object.containsKey(key)) {
                throw new IllegalArgumentException(path + " is missing required field " + key);
            }
        }
        for (String key : object.keySet()) {
            if (!required.contains(key) && !optional.contains(key)) {
                throw new IllegalArgumentException(path + " contains unknown field " + key);
            }
        }
    }

    private static void validateExtensions(Object value, String path) {
        if (value == null) {
            return;
        }
        Map<String, Object> extensions = requireMap(value, path);
        for (String name : extensions.keySet()) {
            if (!EXTENSION_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException(path + " contains invalid namespace " + name);
            }
        }
    }

    private static void requireExactString(
            Map<String, Object> object,
            String field,
            String expected,
            String path
    ) {
        String actual = requireString(object, field, path);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(path + "." + field + " must equal " + expected);
        }
    }

    private static void requireExactStringInSet(
            Map<String, Object> object,
            String field,
            Set<String> allowed,
            String path
    ) {
        String actual = requireString(object, field, path);
        if (!allowed.contains(actual)) {
            throw new IllegalArgumentException(path + "." + field + " has an unsupported value");
        }
    }

    private static String requireString(Map<String, Object> object, String field, String path) {
        Object value = object.get(field);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(path + "." + field + " must be a string");
        }
        return string;
    }

    private static void optionalString(Map<String, Object> object, String field, String path) {
        if (object.containsKey(field)) {
            requireString(object, field, path);
        }
    }

    private static List<Object> requireList(Map<String, Object> object, String field, String path) {
        Object value = object.get(field);
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + "." + field + " must be an array");
        }
        return new ArrayList<>(list);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        return (Map<String, Object>) map;
    }

    private static Map<String, Object> freezeMap(Map<?, ?> input, String path) {
        Objects.requireNonNull(input, path);
        Map<String, Object> frozen = new TreeMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            frozen.put(key, freezeValue(entry.getValue(), path + "." + key));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private static Object freezeValue(Object value, String path) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof BigInteger || value instanceof Byte
                || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return value;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.signum() == 0 ? BigDecimal.ZERO : decimal.stripTrailingZeros();
        }
        if (value instanceof Float || value instanceof Double) {
            if (!Double.isFinite(((Number) value).doubleValue())) {
                throw new IllegalArgumentException(path + " contains a non-finite number");
            }
            return BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros();
        }
        if (value instanceof Map<?, ?> map) {
            return freezeMap(map, path);
        }
        if (value instanceof List<?> list) {
            List<Object> frozen = new ArrayList<>(list.size());
            for (int index = 0; index < list.size(); index++) {
                frozen.add(freezeValue(list.get(index), path + "[" + index + "]"));
            }
            return List.copyOf(frozen);
        }
        throw new IllegalArgumentException(path + " contains unsupported canonical value "
                + value.getClass().getSimpleName());
    }
}
