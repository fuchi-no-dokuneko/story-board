package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleCanonicalDecimalsFactory {
    static Map<String, BigDecimal> decimals(Object value, String path)  {
        Map<String, Object> raw = StyleCanonical.object(value, path);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        raw.forEach((key, entry) -> {
            if (!(entry instanceof Number number)) {
                throw new IllegalArgumentException(path + "." + key + " must be numeric");
            }
            result.put(key, new BigDecimal(number.toString()));
        });
        return Map.copyOf(result);
    }
}
