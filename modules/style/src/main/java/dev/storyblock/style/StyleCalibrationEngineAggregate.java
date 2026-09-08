package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class StyleCalibrationEngineAggregate {
    static StyleFeatureSet aggregate(List<StyleFeatureSet> featureSets) {
        featureSets = List.copyOf(featureSets);
        if (featureSets.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate zero style feature sets");
        }
        StyleFeatureContract contract = featureSets.getFirst().contract();
        String contractHash = contract.contractHash();
        boolean hasEmbedding = StyleCalibrationEngineHasChannel.hasChannel(
                featureSets.getFirst(), StyleFeatureChannel.OPTIONAL_EMBEDDING
        );
        for (StyleFeatureSet featureSet : featureSets) {
            if (!contractHash.equals(featureSet.contract().contractHash())
                    || hasEmbedding != StyleCalibrationEngineHasChannel.hasChannel(
                            featureSet, StyleFeatureChannel.OPTIONAL_EMBEDDING
                    )) {
                throw new IllegalArgumentException(
                        "Aggregated style features must use one complete contract"
                );
            }
        }

        List<StyleFeatureVector> vectors = new ArrayList<>();
        for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
            if (channel == StyleFeatureChannel.OPTIONAL_EMBEDDING && !hasEmbedding) {
                continue;
            }
            List<StyleFeatureVector> source = featureSets.stream()
                    .map(featureSet -> featureSet.require(channel))
                    .toList();
            vectors.add(new StyleFeatureVector(
                    channel,
                    channel.featureVersion(),
                    contractHash,
                    channel == StyleFeatureChannel.OPTIONAL_EMBEDDING
                            ? Map.of()
                            : StyleCalibrationEngineAveragedDistribution.averagedDistribution(source, contract.topK()),
                    channel == StyleFeatureChannel.OPTIONAL_EMBEDDING
                            ? Map.of()
                            : StyleCalibrationEngineAveragedMeasurements.averagedMeasurements(source),
                    channel == StyleFeatureChannel.OPTIONAL_EMBEDDING
                            ? StyleCalibrationEngineAveragedEmbedding.averagedEmbedding(source)
                            : List.of()
            ));
        }
        List<String> componentHashes = featureSets.stream()
                .map(StyleFeatureSet::featureSetHash)
                .sorted()
                .toList();
        return new StyleFeatureSet(
                CanonicalJson.hash(componentHashes), contract, vectors
        );
    }
}
