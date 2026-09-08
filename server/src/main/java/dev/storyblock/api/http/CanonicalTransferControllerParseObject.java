package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalPackageException;
import java.util.Map;

final class CanonicalTransferControllerParseObject {
    static Map<String, Object> parseObject(byte[] value, String path) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = CanonicalJson.mapper().readValue(value, Map.class);
            return CanonicalRequestObject.object(parsed, path);
        } catch (CanonicalPackageException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new CanonicalPackageException(path + " is malformed", failure);
        }
    }
}
