package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.style.StyleFeatureAnalyzer.SCALE;

final class StyleFeatureAnalyzerDistribution {
    static Map<String, BigDecimal> distribution(
            Map<String, Long> rawCounts,
            int topK
    ) {
        if (rawCounts.isEmpty()) {
            return Map.of("OTHER", BigDecimal.ONE);
        }
        List<Map.Entry<String, Long>> ordered = rawCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        Map<String, Long> retained = new LinkedHashMap<>();
        long other = 0;
        for (int index = 0; index < ordered.size(); index++) {
            Map.Entry<String, Long> entry = ordered.get(index);
            if (index < topK) {
                retained.put(entry.getKey(), entry.getValue());
            } else {
                other += entry.getValue();
            }
        }
        if (other > 0) {
            retained.put("OTHER", other);
        }
        long total = retained.values().stream().mapToLong(Long::longValue).sum();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        retained.forEach((key, count) -> result.put(
                key,
                BigDecimal.valueOf(count).divide(
                        BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_EVEN
                ).stripTrailingZeros()
        ));
        return Map.copyOf(result);
    }
}
