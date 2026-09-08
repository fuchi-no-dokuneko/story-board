package dev.storyblock.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class StyleFeatureAnalyzerCompareAction {
    static StyleDistanceReport compare(StyleFeatureAnalyzer self, StyleFeatureSet target, StyleFeatureSet current)  {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(current, "current");
        String contractHash = target.contract().contractHash();
        if (!contractHash.equals(current.contract().contractHash())) {
            throw new IllegalArgumentException(
                    "Style feature sets with different contracts cannot be compared"
            );
        }
        boolean targetEmbedding = target.channels().stream().anyMatch(
                vector -> vector.channel() == StyleFeatureChannel.OPTIONAL_EMBEDDING
        );
        boolean currentEmbedding = current.channels().stream().anyMatch(
                vector -> vector.channel() == StyleFeatureChannel.OPTIONAL_EMBEDDING
        );
        if (targetEmbedding != currentEmbedding) {
            throw new IllegalArgumentException(
                    "Optional style embeddings must be present on both sides"
            );
        }

        List<StyleChannelDistance> distances = new ArrayList<>();
        for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
            if (channel == StyleFeatureChannel.OPTIONAL_EMBEDDING && !targetEmbedding) {
                continue;
            }
            StyleFeatureVector baseline = target.require(channel);
            StyleFeatureVector observed = current.require(channel);
            distances.add(StyleFeatureAnalyzerDistance.distance(
                    channel,
                    baseline,
                    observed,
                    target.contract().additiveSmoothingAlpha()
            ));
        }
        return new StyleDistanceReport(
                contractHash,
                target.featureSetHash(),
                current.featureSetHash(),
                distances,
                true
        );
    }
}
