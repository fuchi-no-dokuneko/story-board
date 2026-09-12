package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

final class StyleFeatureAnalyzerVector {
    static StyleFeatureVector vector(
            StyleFeatureChannel channel,
            String contractHash,
            Map<String, BigDecimal> distribution,
            Map<String, BigDecimal> measurements
    ) {
        return new StyleFeatureVector(
                channel,
                channel.featureVersion(),
                contractHash,
                distribution,
                measurements,
                List.of()
        );
    }
}
