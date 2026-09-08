package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleFeatureAnalyzerContributorValues {
    static Map<String, BigDecimal> contributorValues(
            StyleFeatureVector vector
    ) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        vector.distribution().forEach((key, value) ->
                values.put("distribution:" + key, value)
        );
        vector.measurements().forEach((key, value) ->
                values.put("measurement:" + key, value)
        );
        for (int index = 0; index < vector.embedding().size(); index++) {
            values.put("embedding:" + index, vector.embedding().get(index));
        }
        return Map.copyOf(values);
    }
}
