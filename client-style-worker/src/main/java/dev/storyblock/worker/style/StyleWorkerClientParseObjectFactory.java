package dev.storyblock.worker.style;

import dev.storyblock.contracts.CanonicalJson;
import java.util.Map;

final class StyleWorkerClientParseObjectFactory {
    static Map<String, Object> parseObject(byte[] body, String path)  {
        try {
            Object parsed = CanonicalJson.mapper().readValue(body, Map.class);
            if (!(parsed instanceof Map<?, ?> map)
                    || map.keySet().stream().anyMatch(key -> !(key instanceof String))) {
                throw new StyleWorkerProtocolException(path + " must be a JSON object");
            }
            return (Map<String, Object>) map;
        } catch (StyleWorkerProtocolException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new StyleWorkerProtocolException(path + " is malformed", failure);
        }
    }
}
