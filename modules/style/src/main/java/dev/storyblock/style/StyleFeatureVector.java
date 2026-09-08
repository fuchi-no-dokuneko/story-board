package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleFeatureVector(
        StyleFeatureChannel channel,
        String channelVersion,
        String contractHash,
        Map<String, BigDecimal> distribution,
        Map<String, BigDecimal> measurements,
        List<BigDecimal> embedding
) {
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    static final Pattern KEY = Pattern.compile("[^\\p{Cc}]{1,256}");
    static final Set<String> FIELDS = Set.of(
            "channel", "channel_version", "contract_hash", "distribution",
            "measurements", "embedding"
    );

    public StyleFeatureVector {
        Objects.requireNonNull(channel, "channel");
        if (!channel.featureVersion().equals(channelVersion)) {
            throw new IllegalArgumentException("Style channel version is not supported");
        }
        if (contractHash == null || !HASH.matcher(contractHash).matches()) {
            throw new IllegalArgumentException("Style feature contract hash is invalid");
        }
        distribution = StyleFeatureVectorValidatedMap.validatedMap(distribution, true, "distribution");
        measurements = StyleFeatureVectorValidatedMap.validatedMap(measurements, false, "measurements");
        embedding = List.copyOf(embedding);
        for (BigDecimal value : embedding) {
            Objects.requireNonNull(value, "embedding value");
        }
        if (channel == StyleFeatureChannel.OPTIONAL_EMBEDDING) {
            if (!distribution.isEmpty() || !measurements.isEmpty() || embedding.isEmpty()) {
                throw new IllegalArgumentException(
                        "Optional style embedding must contain only vector values"
                );
            }
        } else if (distribution.isEmpty() && measurements.isEmpty()) {
            throw new IllegalArgumentException("Required style channel cannot be empty");
        } else if (!embedding.isEmpty()) {
            throw new IllegalArgumentException("Only the optional channel may contain embedding");
        }
    }

    public static StyleFeatureVector fromCanonical(Map<String, Object> value) {
        return StyleFeatureVectorFromCanonicalFactory.fromCanonical(value);
    }

    public Map<String, Object> canonicalValue() {
        return StyleFeatureVectorCanonicalValueAction.canonicalValue(this);
    }

}
