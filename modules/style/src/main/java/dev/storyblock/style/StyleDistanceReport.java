package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public record StyleDistanceReport(
        String contractHash,
        String targetFeatureSetHash,
        String currentFeatureSetHash,
        List<StyleChannelDistance> channels,
        boolean tokenKlDiagnosticOnly
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public StyleDistanceReport {
        for (String hash : List.of(
                contractHash, targetFeatureSetHash, currentFeatureSetHash
        )) {
            if (hash == null || !HASH.matcher(hash).matches()) {
                throw new IllegalArgumentException("Style distance hash is invalid");
            }
        }
        channels = List.copyOf(channels);
        EnumSet<StyleFeatureChannel> identities = EnumSet.noneOf(StyleFeatureChannel.class);
        for (StyleChannelDistance channel : channels) {
            if (!identities.add(channel.channel())) {
                throw new IllegalArgumentException("Style distance channels must be unique");
            }
        }
        if (!identities.containsAll(StyleFeatureChannel.requiredChannels())) {
            throw new IllegalArgumentException("Style distance report lacks a required channel");
        }
        if (!tokenKlDiagnosticOnly) {
            throw new IllegalArgumentException("Token KL must remain diagnostic-only");
        }
    }

    public boolean hasIndependentPrimaryEvidence() {
        return channels.stream().filter(StyleChannelDistance::independentGateEvidence)
                .count() >= 2;
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channels", channels.stream()
                .map(StyleChannelDistance::canonicalValue).toList());
        value.put("contract_hash", contractHash);
        value.put("current_feature_set_hash", currentFeatureSetHash);
        value.put("target_feature_set_hash", targetFeatureSetHash);
        value.put("token_kl_diagnostic_only", tokenKlDiagnosticOnly);
        return CanonicalValues.freezeMap(value, "style_distance_report");
    }
}
