package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern KEY = Pattern.compile("[^\\p{Cc}]{1,256}");
    private static final Set<String> FIELDS = Set.of(
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
        distribution = validatedMap(distribution, true, "distribution");
        measurements = validatedMap(measurements, false, "measurements");
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
        StyleCanonical.requireKeys(value, FIELDS, "style_feature_vector");
        return new StyleFeatureVector(
                StyleFeatureChannel.fromCanonicalName(StyleCanonical.string(
                        value, "channel", "style_feature_vector"
                )),
                StyleCanonical.string(value, "channel_version", "style_feature_vector"),
                StyleCanonical.string(value, "contract_hash", "style_feature_vector"),
                StyleCanonical.decimals(value.get("distribution"), "style_feature_vector.distribution"),
                StyleCanonical.decimals(value.get("measurements"), "style_feature_vector.measurements"),
                StyleCanonical.decimalList(value.get("embedding"), "style_feature_vector.embedding")
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channel", channel.canonicalName());
        value.put("channel_version", channelVersion);
        value.put("contract_hash", contractHash);
        value.put("distribution", distribution);
        value.put("measurements", measurements);
        value.put("embedding", embedding);
        return CanonicalValues.freezeMap(value, "style_feature_vector");
    }

    private static Map<String, BigDecimal> validatedMap(
            Map<String, BigDecimal> values,
            boolean nonNegative,
            String field
    ) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : Map.copyOf(values).entrySet()) {
            if (!KEY.matcher(entry.getKey()).matches() || entry.getValue() == null
                    || (nonNegative && entry.getValue().signum() < 0)) {
                throw new IllegalArgumentException("Style feature " + field + " is invalid");
            }
            result.put(entry.getKey(), entry.getValue().stripTrailingZeros());
        }
        return Map.copyOf(result);
    }
}
