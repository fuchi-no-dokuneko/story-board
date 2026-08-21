package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StyleChannelDistance(
        StyleFeatureChannel channel,
        String channelVersion,
        StyleDistanceMetric primaryMetric,
        BigDecimal primaryDistance,
        boolean independentGateEvidence,
        Map<String, Object> diagnostics
) {
    public StyleChannelDistance {
        Objects.requireNonNull(channel, "channel");
        if (!channel.featureVersion().equals(channelVersion)
                || channel.primaryMetric() != primaryMetric) {
            throw new IllegalArgumentException("Style channel distance contract is inconsistent");
        }
        if (primaryDistance == null || primaryDistance.signum() < 0) {
            throw new IllegalArgumentException("Style primary distance cannot be negative");
        }
        primaryDistance = primaryDistance.stripTrailingZeros();
        diagnostics = CanonicalValues.freezeMap(diagnostics, "style_channel_distance.diagnostics");
        if (primaryMetric.canonicalName().contains("kl")) {
            throw new IllegalArgumentException("KL cannot be a primary style gate metric");
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channel", channel.canonicalName());
        value.put("channel_version", channelVersion);
        value.put("diagnostics", diagnostics);
        value.put("independent_gate_evidence", independentGateEvidence);
        value.put("primary_distance", primaryDistance);
        value.put("primary_metric", primaryMetric.canonicalName());
        return CanonicalValues.freezeMap(value, "style_channel_distance");
    }
}
