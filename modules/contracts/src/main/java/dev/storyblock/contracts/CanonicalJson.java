package dev.storyblock.contracts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class CanonicalJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private CanonicalJson() {
    }

    public static byte[] bytes(Object value) {
        return MAPPER.writeValueAsBytes(value);
    }

    public static String string(Object value) {
        return new String(bytes(value), StandardCharsets.UTF_8);
    }

    public static String hash(Object value) {
        return hashBytes(bytes(value));
    }

    public static String hashBytes(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static <T> T parse(byte[] json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
