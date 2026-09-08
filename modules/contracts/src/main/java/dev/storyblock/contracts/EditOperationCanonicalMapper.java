package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import java.util.Map;

public final class EditOperationCanonicalMapper {
    EditOperationCanonicalMapper() {
    }

    public static Map<String, Object> toCanonical(EditOperation operation) {
        Map<String, Object> envelope = EditOperationCanonicalMapperContext.context(operation.context());
        envelope.put("type", operation.type().canonicalName());
        envelope.put("payload", EditOperationCanonicalMapperPayload.payload(operation));
        return Map.copyOf(envelope);
    }

    public static String hash(EditOperation operation) {
        return CanonicalJson.hash(toCanonical(operation));
    }

    public static EditOperation fromCanonical(byte[] canonicalJson) {
        @SuppressWarnings("unchecked")
        Map<String, Object> value = CanonicalJson.mapper().readValue(canonicalJson, Map.class);
        return fromCanonical(value);
    }

    public static EditOperation fromCanonical(Map<String, Object> envelope) {
        return EditOperationCanonicalMapperFromCanonicalFactory.fromCanonical(envelope);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

}
