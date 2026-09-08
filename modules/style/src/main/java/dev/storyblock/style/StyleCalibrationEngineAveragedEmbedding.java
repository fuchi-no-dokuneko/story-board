package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.style.StyleCalibrationEngine.SCALE;

final class StyleCalibrationEngineAveragedEmbedding {
    static List<BigDecimal> averagedEmbedding(
            List<StyleFeatureVector> vectors
    ) {
        int dimensions = vectors.getFirst().embedding().size();
        if (vectors.stream().anyMatch(vector ->
                vector.embedding().size() != dimensions
        )) {
            throw new IllegalArgumentException(
                    "Style calibration embedding dimensions must match"
            );
        }
        List<BigDecimal> result = new ArrayList<>();
        for (int dimension = 0; dimension < dimensions; dimension++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (StyleFeatureVector vector : vectors) {
                sum = sum.add(vector.embedding().get(dimension));
            }
            result.add(sum.divide(
                    BigDecimal.valueOf(vectors.size()),
                    SCALE,
                    RoundingMode.HALF_EVEN
            ).stripTrailingZeros());
        }
        return List.copyOf(result);
    }
}
