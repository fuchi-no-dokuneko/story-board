package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleFeatureSet(
        String sourceHash,
        StyleFeatureContract contract,
        List<StyleFeatureVector> channels
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of("source_hash", "contract", "channels");

    public StyleFeatureSet {
        if (sourceHash == null || !HASH.matcher(sourceHash).matches()) {
            throw new IllegalArgumentException("Style feature source hash is invalid");
        }
        java.util.Objects.requireNonNull(contract, "contract");
        channels = List.copyOf(channels);
        EnumSet<StyleFeatureChannel> identities = EnumSet.noneOf(StyleFeatureChannel.class);
        for (StyleFeatureVector vector : channels) {
            if (!identities.add(vector.channel())
                    || !contract.contractHash().equals(vector.contractHash())) {
                throw new IllegalArgumentException(
                        "Style feature channels must be unique and match their contract"
                );
            }
        }
        if (!identities.containsAll(StyleFeatureChannel.requiredChannels())) {
            throw new IllegalArgumentException("Style feature set is missing a required channel");
        }
    }

    public static StyleFeatureSet fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_feature_set");
        return new StyleFeatureSet(
                StyleCanonical.string(value, "source_hash", "style_feature_set"),
                StyleFeatureContract.fromCanonical(StyleCanonical.object(
                        value.get("contract"), "style_feature_set.contract"
                )),
                StyleCanonical.objects(value.get("channels"), "style_feature_set.channels")
                        .stream().map(StyleFeatureVector::fromCanonical).toList()
        );
    }

    public StyleFeatureVector require(StyleFeatureChannel channel) {
        return channels.stream()
                .filter(vector -> vector.channel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Style feature set does not contain " + channel.canonicalName()
                ));
    }

    public String featureSetHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channels", channels.stream()
                .map(StyleFeatureVector::canonicalValue).toList());
        value.put("contract", contract.canonicalValue());
        value.put("source_hash", sourceHash);
        return CanonicalValues.freezeMap(value, "style_feature_set");
    }
}
