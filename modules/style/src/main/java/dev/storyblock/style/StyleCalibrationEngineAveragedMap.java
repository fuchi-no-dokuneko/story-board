package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.style.StyleCalibrationEngine.SCALE;

final class StyleCalibrationEngineAveragedMap {
    static Map<String, BigDecimal> averagedMap(
            List<Map<String, BigDecimal>> values,
            int count
    ) {
        Set<String> keys = new LinkedHashSet<>();
        values.forEach(value -> keys.addAll(value.keySet()));
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String key : keys) {
            BigDecimal sum = values.stream()
                    .map(value -> value.getOrDefault(key, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(key, sum.divide(
                    BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_EVEN
            ).stripTrailingZeros());
        }
        return Map.copyOf(result);
    }
}
