package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

final class StyleCalibrationEngineAveragedMeasurements {
    static Map<String, BigDecimal> averagedMeasurements(
            List<StyleFeatureVector> vectors
    ) {
        return StyleCalibrationEngineAveragedMap.averagedMap(
                vectors.stream().map(StyleFeatureVector::measurements).toList(),
                vectors.size()
        );
    }
}
