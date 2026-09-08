package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static dev.storyblock.style.StyleFeatureVector.KEY;

final class StyleFeatureVectorValidatedMap {
    static Map<String, BigDecimal> validatedMap(
            Map<String, BigDecimal> values,
            boolean nonNegative,
            String field
    ) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : Map.copyOf(values).entrySet()) {
            if (!KEY.matcher(entry.getKey()).matches() || entry.getValue() == null
                    || (nonNegative && entry.getValue().signum() < 0)) {
                throw new IllegalArgumentException("Style feature " + field + " is invalid");
            }
            result.put(entry.getKey(), entry.getValue().stripTrailingZeros());
        }
        return Map.copyOf(result);
    }
}
