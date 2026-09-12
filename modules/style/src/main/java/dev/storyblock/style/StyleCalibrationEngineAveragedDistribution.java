package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StyleCalibrationEngineAveragedDistribution {
    static Map<String, BigDecimal> averagedDistribution(
            List<StyleFeatureVector> vectors,
            int topK
    ) {
        Map<String, BigDecimal> average = StyleCalibrationEngineAveragedMap.averagedMap(
                vectors.stream().map(StyleFeatureVector::distribution).toList(),
                vectors.size()
        );
        BigDecimal other = average.getOrDefault("OTHER", BigDecimal.ZERO);
        List<Map.Entry<String, BigDecimal>> ordered = average.entrySet().stream()
                .filter(entry -> !"OTHER".equals(entry.getKey()))
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            Map.Entry<String, BigDecimal> entry = ordered.get(index);
            if (index < topK) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                other = other.add(entry.getValue());
            }
        }
        if (other.signum() > 0 || result.isEmpty()) {
            result.put("OTHER", other.signum() == 0 ? BigDecimal.ONE : other);
        }
        return Map.copyOf(result);
    }
}
